package com.iqiyi.android.qigsaw.core.splitload.compat;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.AbiUtil;


public class NativePathMapperImpl implements NativePathMapper {

  private final NativePathMapper mapper;

  public NativePathMapperImpl(Context context) {
    if (needUseCommonSoDir(context)) {
      mapper = new PathMapperV21(context);
    } else {
      mapper = new PathMapperAbove21(context);
    }
  }

  @Override
  public String map(String splitName, String originPath) {
    if (TextUtils.isEmpty(splitName) || TextUtils.isEmpty(originPath)) {
      return originPath;
    }
    synchronized(Runtime.getRuntime()) {
      return mapper.map(splitName, originPath);
    }
  }

  private boolean needUseCommonSoDir(Context context) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.M
        && AbiUtil.isArm64(context);
  }

}
