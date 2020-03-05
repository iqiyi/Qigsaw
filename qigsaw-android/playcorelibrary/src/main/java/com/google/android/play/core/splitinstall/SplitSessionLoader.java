package com.google.android.play.core.splitinstall;

import android.content.Intent;
import androidx.annotation.RestrictTo;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface SplitSessionLoader {

    void load(List<Intent> splitFileIntents, SplitSessionStatusChanger changer);

}
