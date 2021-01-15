package com.iqiyi.android.qigsaw.core.splitload.compat;

import android.content.Context;

class PathMapperAbove21 implements NativePathMapper {

  private final Context context;

  PathMapperAbove21(Context context) {
    this.context = context;
  }

  @Override
  public String map(String splitName, String originPath) {
    return originPath;
  }

}
