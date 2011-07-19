/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.hoop.fs;

import com.cloudera.hoop.HoopServer;
import com.cloudera.lib.service.Hadoop;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.util.regex.Pattern;

public class FSListStatus implements Hadoop.FileSystemExecutor<JSONArray>, PathFilter {
  private Path path;
  private PathFilter filter;

  public FSListStatus(String path, String filter) {
    this.path = new Path(path);
    this.filter = (filter == null) ? this : new GlobFilter(filter);
  }

  @Override
  public JSONArray execute(FileSystem fs) throws IOException {
    FileStatus[] status = fs.listStatus(path, filter);
    String httpBaseUrl = HoopServer.get().getBaseUrl();
    return FSUtils.fileStatusToJSON(status, httpBaseUrl);
  }

  @Override
  public boolean accept(Path path) {
    return true;
  }

  /**
   * Cut&Paste of Hadoop FileSystem.GlobFilter private class.
   */
  private static class GlobFilter implements PathFilter {
    private Pattern regex;

    /**
     * Default pattern character: Escape any special meaning.
     */
    private static final char PAT_ESCAPE = '\\';
    /**
     * Default pattern character: Any single character.
     */
    private static final char PAT_ANY = '.';
    /**
     * Default pattern character: Character set close.
     */
    private static final char PAT_SET_CLOSE = ']';

    GlobFilter(String filePattern) {
      setRegex(filePattern);
    }

    private boolean isJavaRegexSpecialChar(char pChar) {
      return pChar == '.' || pChar == '$' || pChar == '(' || pChar == ')' ||
             pChar == '|' || pChar == '+';
    }

    void setRegex(String filePattern) {
      int len;
      int setOpen;
      int curlyOpen;
      boolean setRange;

      StringBuilder fileRegex = new StringBuilder();

      // Validate the pattern
      len = filePattern.length();
      if (len == 0) {
        return;
      }

      setOpen = 0;
      setRange = false;
      curlyOpen = 0;

      for (int i = 0; i < len; i++) {
        char pCh;

        // Examine a single pattern character
        pCh = filePattern.charAt(i);
        if (pCh == PAT_ESCAPE) {
          fileRegex.append(pCh);
          i++;
          if (i >= len) {
            error("An escaped character does not present", filePattern, i);
          }
          pCh = filePattern.charAt(i);
        }
        else if (isJavaRegexSpecialChar(pCh)) {
          fileRegex.append(PAT_ESCAPE);
        }
        else if (pCh == '*') {
          fileRegex.append(PAT_ANY);
        }
        else if (pCh == '?') {
          pCh = PAT_ANY;
        }
        else if (pCh == '{') {
          fileRegex.append('(');
          pCh = '(';
          curlyOpen++;
        }
        else if (pCh == ',' && curlyOpen > 0) {
          fileRegex.append(")|");
          pCh = '(';
        }
        else if (pCh == '}' && curlyOpen > 0) {
          // End of a group
          curlyOpen--;
          fileRegex.append(")");
          pCh = ')';
        }
        else if (pCh == '[' && setOpen == 0) {
          setOpen++;
        }
        else if (pCh == '^' && setOpen > 0) {
        }
        else if (pCh == '-' && setOpen > 0) {
          // Character set range
          setRange = true;
        }
        else if (pCh == PAT_SET_CLOSE && setRange) {
          // Incomplete character set range
          error("Incomplete character set range", filePattern, i);
        }
        else if (pCh == PAT_SET_CLOSE && setOpen > 0) {
          // End of a character set
          if (setOpen < 2) {
            error("Unexpected end of set", filePattern, i);
          }
          setOpen = 0;
        }
        else if (setOpen > 0) {
          // Normal character, or the end of a character set range
          setOpen++;
          setRange = false;
        }
        fileRegex.append(pCh);
      }

      // Check for a well-formed pattern
      if (setOpen > 0 || setRange || curlyOpen > 0) {
        // Incomplete character set or character range
        error("Expecting set closure character or end of range, or }",
              filePattern, len);
      }
      regex = Pattern.compile(fileRegex.toString());
    }

    public boolean accept(Path path) {
      return regex.matcher(path.getName()).matches();
    }

    private void error(String s, String pattern, int pos) {
      throw new IllegalArgumentException("Illegal file pattern: " + s + " for glob " + pattern + " at " + pos);
    }
  }

}
