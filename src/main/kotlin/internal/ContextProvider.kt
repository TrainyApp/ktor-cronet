package com.trainyapp.cronet.internal

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri

//https://andretietz.com/2017/09/06/autoinitialise-android-library/
@PublishedApi
internal class ContextProvider : ContentProvider() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var ANDROID_CONTEXT: Context? = null
    }

    override fun attachInfo(context: Context, info: ProviderInfo?) {
        if (info == null) {
            throw NullPointerException("AndroidContextProvider ProviderInfo cannot be null.")
        }
        // So if the authorities equal the library internal ones, the developer forgot to set his applicationId
        if ("com.trainyapp.cronet" == info.authority) {
            throw IllegalStateException("Incorrect provider authority in manifest. Most likely due to a "
                + "missing applicationId variable your application\'s build.gradle.")
        }

        super.attachInfo(context, info)
    }

    override fun onCreate(): Boolean {
        ANDROID_CONTEXT = context

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int = 0

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String?>?
    ): Int = 0

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
}