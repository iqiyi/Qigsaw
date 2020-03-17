package com.google.android.play.core.splitinstall;

import android.content.Intent;
import android.support.annotation.RestrictTo;

import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface SplitSessionLoader {

    void load(List<Intent> splitFileIntents, SplitSessionStatusChanger changer);

}
