package com.openhtmltopdf.outputdevice.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class FontFaceFontSupplier implements FSSupplier<InputStream> {
    private final String src;
    private final SharedContext ctx;
    
    public FontFaceFontSupplier(SharedContext ctx, String src) {
        this.src = src;
        this.ctx = ctx;
    }
    
    @Override
    public InputStream supply() {
        byte[] font1 = ctx.getUserAgentCallback().getBinaryResource(src, ExternalResourceType.FONT);
        
        if (font1 == null) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT_FACE, src);
            return null;
        }
        
        return new ByteArrayInputStream(font1);
    }
}
