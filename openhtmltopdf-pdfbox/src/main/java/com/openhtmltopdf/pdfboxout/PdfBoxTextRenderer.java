/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.pdfboxout.PdfBoxUtil.FontRun;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxTextRenderer implements TextRenderer {
    private static float TEXT_MEASURING_DELTA = 0.01f;

    private BidiReorderer _reorderer;

    // These will mean only first missing font/metrics
    // is logged but they should have already got a loading warning.
    // Quiet the font is missing log message.
    private boolean _loggedMissingFont = false;

    // Quiet the font metrics not available message.
    private boolean _loggedMissingMetrics = false;

    public void setup(FontContext context, BidiReorderer reorderer) {
        this._reorderer = reorderer;
    }

    @Override
    public void drawString(OutputDevice outputDevice, String string, float x, float y) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, null);
    }
    
    @Override
    public void drawString(
            OutputDevice outputDevice, String string, float x, float y, JustificationInfo info) {
        ((PdfBoxOutputDevice)outputDevice).drawString(string, x, y, info);
    }

    @Override
    public FSFontMetrics getFSFontMetrics(FontContext context, FSFont font, String string) {
        List<FontDescription> descrs = ((PdfBoxFSFont) font).getFontDescription();
        float size = font.getSize2D();
        PdfBoxFSFontMetrics result = new PdfBoxFSFontMetrics();
        
            float largestAscent = -Float.MAX_VALUE;
            float largestDescent = -Float.MAX_VALUE;
            float largestStrikethroughOffset = -Float.MAX_VALUE;
            float largestStrikethroughThickness = -Float.MAX_VALUE;
            float largestUnderlinePosition = -Float.MAX_VALUE;
            float largestUnderlineThickness = -Float.MAX_VALUE;
            
            for (FontDescription des : descrs) {
                PdfBoxRawPDFontMetrics metrics = des.getFontMetrics();

                if (metrics == null) {
                    if (!_loggedMissingMetrics) {
                        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_FONT_METRICS_NOT_AVAILABLE, des);
                        _loggedMissingMetrics = true;
                    }
                    continue;
                }
                
                float loopAscent = metrics._ascent;
                float loopDescent = metrics._descent;
                float loopStrikethroughOffset = metrics._strikethroughOffset;
                float loopStrikethroughThickness = metrics._strikethroughThickness;
                float loopUnderlinePosition = metrics._underlinePosition;
                float loopUnderlineThickness = metrics._underlineThickness;
                
                if (loopAscent > largestAscent) {
                    largestAscent = loopAscent;
                }
                
                if (loopDescent > largestDescent) {
                    largestDescent = loopDescent; 
                }
                
                if (loopStrikethroughOffset > largestStrikethroughOffset) {
                    largestStrikethroughOffset = loopStrikethroughOffset;
                }
                
                if (loopStrikethroughThickness > largestStrikethroughThickness) {
                    largestStrikethroughThickness = loopStrikethroughThickness;
                }
                
                if (loopUnderlinePosition > largestUnderlinePosition) {
                    largestUnderlinePosition = loopUnderlinePosition;
                }
                
                if (loopUnderlineThickness > largestUnderlineThickness) {
                    largestUnderlineThickness = loopUnderlineThickness;
                }
            }
            
            result.setAscent(largestAscent / 1000f * size);
            result.setDescent(largestDescent / 1000f * size);
            result.setStrikethroughOffset(largestStrikethroughOffset / 1000f * size);
            
            if (largestStrikethroughThickness > 0) {
                result.setStrikethroughThickness(largestStrikethroughThickness / 1000f * size);
            } else {
                result.setStrikethroughThickness(size / 12.0f);
            }
            
            result.setUnderlineOffset(largestUnderlinePosition / 1000f * size);
            result.setUnderlineThickness(largestUnderlineThickness / 1000f * size);

        return result;
    }

    private static class ReplacementChar {
        String replacement;
        FontDescription fontDescription;
    }
    
    public static boolean isJustificationSpace(int c) {
        return c == ' ' || c == '\u00a0' || c == '\u3000';
    }
    
    private static ReplacementChar getReplacementChar(FSFont font) {
        String replaceStr = ThreadCtx.get().sharedContext().getReplacementText();
        List<FontDescription> descriptions = ((PdfBoxFSFont) font).getFontDescription();
        
        for (FontDescription des : descriptions) {
            try {
                des.getFont().getStringWidth(replaceStr);

                // Got here without throwing, so the text exists in font.
                ReplacementChar replace = new ReplacementChar();
                replace.replacement = replaceStr;
                replace.fontDescription = des;
                return replace;
            } catch (Exception e)
            {
                // Could not use replacement character in this font.
            }
        }

        // Still haven't found a font supporting our replacement text, try space character.
        replaceStr = " ";
        for (FontDescription des : descriptions) {
            try {
                des.getFont().getStringWidth(replaceStr);

                // Got here without throwing, so the char exists in font.
                ReplacementChar replace = new ReplacementChar();
                replace.replacement = " ";
                replace.fontDescription = des;
                return replace;
            } catch (Exception e)
            {
                // Could not use space in this font!
            }
        }
    
        // Really?, no font support for either replacement text or space!
        XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.GENERAL_PDF_SPECIFIED_FONTS_DONT_CONTAIN_A_SPACE_CHARACTER);
        ReplacementChar replace = new ReplacementChar();
        replace.replacement = "";
        replace.fontDescription = descriptions.get(0);
        return replace;
    }
    
    public static List<FontRun> divideIntoFontRuns(FSFont font, String str, BidiReorderer reorderer) {
        StringBuilder sb = new StringBuilder();
        ReplacementChar replace = PdfBoxTextRenderer.getReplacementChar(font);
        List<FontDescription> fonts = ((PdfBoxFSFont) font).getFontDescription();
        List<FontRun> runs = new ArrayList<>();
        FontRun current = new FontRun();
        
        for (int i = 0; i < str.length(); ) {
            int unicode = str.codePointAt(i);
            i += Character.charCount(unicode);
            String ch = String.valueOf(Character.toChars(unicode));

            if (!OpenUtil.isSafeFontCodePointToPrint(unicode)) {
                // Filter out characters that should never be visible (such
                // as soft-hyphen) but are in some fonts.
                continue;
            }

            boolean gotChar = false;
            
            FONT_LOOP:
            for (FontDescription des : fonts) {
                try {
                    des.getFont().getStringWidth(ch);
                    // We got here, so this font has this character.
                    if (current.des == null) {
                        // First character of run.
                        current.des = des;
                    }
                    else if (des != current.des) {
                        // We have changed font, so we'll start a new font run.
                        current.str = sb.toString();
                        runs.add(current);
                        current = new FontRun();
                        current.des = des;
                        sb = new StringBuilder();
                    }

                    if (isJustificationSpace(unicode)) {
                        current.spaceCharacterCount++;
                    } else {
                        current.otherCharacterCount++;
                    }
                    
                    sb.append(ch);
                    gotChar = true;
                    break;
                }
                catch (Exception e1) {
                    if (reorderer.isLiveImplementation()) {
                        // Character is not in font! Next, we try deshaping.
                        String deshaped = reorderer.deshapeText(ch);
                        try {
                            des.getFont().getStringWidth(deshaped);
                            // We got here, so this font has this deshaped character.
                            if (current.des == null) {
                                // First character of run.
                                current.des = des;
                            }
                            else if (des != current.des) {
                                // We have changed font, so we'll start a new font run.
                                current.str = sb.toString();
                                runs.add(current);
                                current = new FontRun();
                                current.des = des;
                                sb = new StringBuilder();
                            }
                            
                            if (isJustificationSpace(unicode)) {
                                current.spaceCharacterCount++;
                            } else {
                                current.otherCharacterCount++;
                            }
                            
                            sb.append(deshaped);
                            gotChar = true;
                            break FONT_LOOP;
                        }
                        catch (Exception e2) {
                            // Keep trying with next font.
                        }
                    }
                }
            }

            if (!gotChar) {
                if (!OpenUtil.isCodePointPrintable(unicode)) {
                    // Filter out control, etc characters when they
                    // are not present in any font.
                    continue;
                }

                // We still don't have the character after all that. So use replacement character.
                if (current.des == null) {
                    // First character of run.
                    current.des = replace.fontDescription;
                }
                else if (replace.fontDescription != current.des) {
                    // We have changed font, so we'll start a new font run.
                    current.str = sb.toString();
                    runs.add(current);
                    current = new FontRun();
                    current.des = replace.fontDescription;
                    sb = new StringBuilder();
                }
                
                if (Character.isSpaceChar(unicode) || Character.isWhitespace(unicode)) {
                    current.spaceCharacterCount++;
                    sb.append(' ');
                }
                else {
                    current.otherCharacterCount++;
                    sb.append(replace.replacement);
                }
            }
        }

        if (sb.length() > 0) {
            current.str = sb.toString();
            runs.add(current);
        }
        
        return runs;
    }
    
    private float getStringWidthSlow(FSFont bf, String str) {
        List<FontRun> runs = divideIntoFontRuns(bf, str, _reorderer);
        float strWidth = 0;
        
        for (FontRun run : runs) {
            try {
                strWidth += run.des.getFont().getStringWidth(run.str);
            } catch (Exception e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.RENDER_BUG_FONT_DIDNT_CONTAIN_EXPECTED_CHARACTER, e);
            }
        }

        return strWidth;
    }

    @Override
    public int getWidth(FontContext context, FSFont font, String string) {
        float result = 0f;

        String effectiveString = TextRenderer.getEffectivePrintableString(string);

        try {
            if (((PdfBoxFSFont) font).getFontDescription() == null 
                    || ((PdfBoxFSFont) font).getFontDescription().isEmpty()) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.RENDER_FONT_LIST_IS_EMPTY);
            } else {
              // Go through the list of font descriptions
              for (FontDescription fd : ((PdfBoxFSFont) font).getFontDescription()) {
                 if (fd.getFont() != null) {
                   result = fd.getFont().getStringWidth(effectiveString) / 1000f * font.getSize2D();
                   break;
                 } else {
                     if (!_loggedMissingFont) {
                         XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.RENDER_FONT_IS_NULL, fd);
                         _loggedMissingFont = true;
                     }
                 }
              }
            }
        } catch (IllegalArgumentException e2) {
            // PDFont::getStringWidth throws an IllegalArgumentException if the character doesn't exist in the font.
            // So we do it one character by character instead.
            result = getStringWidthSlow(font, effectiveString) / 1000f * font.getSize2D();
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("getWidth", e);
        }

        if (result - Math.floor(result) < TEXT_MEASURING_DELTA) {
            return (int)result;
        } else {
            return (int)Math.ceil(result); 
        }
    }

    @Override
    public void setup(FontContext context) {
    }
}
