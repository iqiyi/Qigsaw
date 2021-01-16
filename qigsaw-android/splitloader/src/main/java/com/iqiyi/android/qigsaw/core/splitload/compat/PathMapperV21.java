package com.iqiyi.android.qigsaw.core.splitload.compat;

import java.io.File;
import java.io.FilenameFilter;

import android.content.Context;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import androidx.annotation.RequiresApi;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

/**
 * compat library name too long on android 5.x
 * ref: https://cs.android.com/android/platform/superproject/+/android-5.1.1_r38:bionic/linker/linker.cpp
 * method: soinfo_alloc
 *
 * #define SOINFO_NAME_LEN 128
 *
 *   if (strlen(name) >= SOINFO_NAME_LEN) {
 *     DL_ERR("library name \"%s\" too long", name);
 *     return nullptr;
 *   }
 *
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class PathMapperV21 implements NativePathMapper {

  private static final String TAG = "Split:PathMapper";
  private static final int MAX_LIB_PATH = 128;

  private final Context context;
  private final File commonDir;


  PathMapperV21(Context context) {
    this.context = context;
    commonDir = SplitPathManager.require().getCommonSoDir();
  }

  @Override
  public String map(String splitName, String originPath) {
    boolean isNeedMap = checkIfNeedMapPath(originPath);
    if (!isNeedMap) {
      SplitLog.d(TAG, "do not need map native lib path: %s", originPath);
      return originPath;
    }
    if (!commonDir.exists()) {
      boolean mkdirResult = commonDir.mkdirs();
      if (!mkdirResult) {
        SplitLog.d(TAG, "mkdir: %s failed", commonDir.getAbsolutePath());
        return originPath;
      }
    }
    File targetFile = new File(commonDir, splitName);
    boolean linkResult = symLink(new File(originPath), targetFile, false);
    if (linkResult) {
      return targetFile.getAbsolutePath();
    } else {
      return originPath;
    }
  }

  private boolean checkIfNeedMapPath(String libPath) {
    if (TextUtils.isEmpty(libPath)) {
      return false;
    }
    File libDir = new File(libPath);
    if (!libDir.exists()) {
      return false;
    }
    File[] soFileArray = libDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !TextUtils.isEmpty(name) && name.endsWith(SplitConstants.DOT_SO);
      }
    });
    if (soFileArray == null || soFileArray.length == 0) {
      return false;
    }
    for (File soFile : soFileArray) {
      if (soFile != null && !TextUtils.isEmpty(soFile.getAbsolutePath()) && soFile.getAbsolutePath().length() >= MAX_LIB_PATH) {
        SplitLog.d(TAG, "need map native lib path: %s length >= %d", commonDir.getAbsolutePath(), MAX_LIB_PATH);
        return true;
      }
    }
    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private boolean symLink(File source, File target, boolean isRetry) {
    String sourcePath = source.getAbsolutePath();
    if (!source.exists()) {
      SplitLog.e(TAG, "symLink source: " + sourcePath + " not exist");
      return false;
    }
    String targetPath = target.getAbsolutePath();
    if (target.exists()) {
      boolean isPathEqual = isSymlinkFileEqual(sourcePath, targetPath);
      if (isPathEqual) {
        return true;
      } else {
        if (!target.delete()) {
          SplitLog.e(TAG, "delete symLink target: " + targetPath+ " fail");
          return false;
        }
      }
    }

    try {
      Os.symlink(sourcePath, targetPath);
    } catch (Throwable e) {
      e.printStackTrace();
      if (e instanceof ErrnoException && ((ErrnoException) e).errno == OsConstants.EEXIST) {
        SplitLog.d(TAG, "create symLink exist, from: " + sourcePath + " to: " + targetPath);
        boolean isPathEqual = isSymlinkFileEqual(sourcePath, targetPath);
        if (isPathEqual) {
          return true;
        } else {
          SplitLog.d(TAG, "delete exist symLink, " + " targetPath: " + targetPath);
          target.delete();
          if (!isRetry) {
            return symLink(source, target, true);
          }
        }
      }
      return false;
    }
    SplitLog.d(TAG, "create symLink success from: " + sourcePath + " to: " + targetPath);
    return true;
  }

  private boolean isSymlinkFileEqual(String sourcePath, String targetPath) {
    SplitLog
        .d(TAG, "isSymlinkFileEqual, " + " sourcePath: " + sourcePath + " targetPath: " + targetPath);
    try {
      String oldSourcePath = Os.readlink(targetPath);
      SplitLog.d(TAG, "isSymlinkFileEqual, " + " sourcePath: " + sourcePath + " oldSourcePath: " + oldSourcePath);
      if (!TextUtils.isEmpty(oldSourcePath)) {
        return oldSourcePath.equals(sourcePath);
      }
    } catch (ErrnoException e) {
      e.printStackTrace();
    }
    return false;
  }

}
