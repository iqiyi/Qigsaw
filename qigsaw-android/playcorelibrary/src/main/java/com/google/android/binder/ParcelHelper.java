package com.google.android.binder;

import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class ParcelHelper {

    static {
        ParcelHelper.class.getClassLoader();
    }

    public static <T extends Parcelable> T createFromParcel(Parcel data, Parcelable.Creator<T> creator) {
        return data.readInt() == 0 ? null : creator.createFromParcel(data);
    }

    public static void writeToParcel(Parcel data, Parcelable arg) {
        if (arg == null) {
            data.writeInt(0);
        } else {
            data.writeInt(1);
            arg.writeToParcel(data, 0);
        }
    }

    public static void writeStrongBinder(Parcel data, IInterface iin) {
        if (iin == null) {
            data.writeStrongBinder(null);
        } else {
            data.writeStrongBinder(iin.asBinder());
        }
    }

}
