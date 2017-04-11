package com.openhtmltopdf.outputdevice.helper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.util.XRLog;

public class FontFileFontSupplier implements FSSupplier<InputStream>{
    private final String path;
    
    public FontFileFontSupplier(String path) {
        this.path = path;
    }
    
    @Override
    public InputStream supply() {
        try {
            return new FileInputStream(this.path);
        } catch (FileNotFoundException e) {
            XRLog.exception("While trying to add font from directory, file seems to have disappeared.");
            return null;
        }
    }
}
