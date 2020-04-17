package com.iqiyi.qigsaw.sample.downloader;


import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;


public class DeleteDownloadedFilesTask implements Runnable {

    private static final String TAG = "Split:DeleteFilesTask";

    /**
     * regular expression
     */
    private final String regEx;

    /**
     * path of file that need to be deleted
     */
    private final String dirPath;

    /**
     * true means prefix, false means suffix
     */
    private final boolean isPrefix;


    public DeleteDownloadedFilesTask(String dirPath, boolean isPrefix, String regEx) {
        this.regEx = regEx;
        this.dirPath = dirPath;
        this.isPrefix = isPrefix;
    }

    /**
     * Enumerate and delete all eligible files.
     */
    private void enumAllFileList() {
        if (!TextUtils.isEmpty(dirPath)) {
            File adDir = new File(dirPath);
            if (adDir.exists() && adDir.isDirectory()) {
                if (!TextUtils.isEmpty(regEx)) {
                    DeleteFileFilter filter = new DeleteFileFilter(isPrefix, regEx);
                    //mapping the files that need to be deleted
                    File[] fileList = adDir.listFiles(filter);
                    if (fileList != null && fileList.length > 0) {
                        for (File file : fileList) {
                            if (file.isFile() && file.exists()) {
                                boolean delete = file.delete();
                                Log.i(TAG, "delete assigned group download file:" + regEx + (delete ? "true." : "false!"));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        enumAllFileList();
    }

    /**
     * Filters for filenames beginning or end with XXX
     */
    class DeleteFileFilter implements FilenameFilter {

        private boolean isPrefix;
        private String regEx; // the regEx of prefix or suffix

        DeleteFileFilter(boolean isPrefix, @NonNull String regEx) {
            this.isPrefix = isPrefix;
            this.regEx = regEx;
        }

        @Override
        public boolean accept(File file, String s) {
            return isPrefix ? s.startsWith(regEx) : s.endsWith(regEx);
        }
    }
}



