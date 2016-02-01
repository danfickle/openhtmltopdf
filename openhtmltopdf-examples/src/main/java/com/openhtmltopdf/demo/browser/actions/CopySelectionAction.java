package com.openhtmltopdf.demo.browser.actions;

import com.openhtmltopdf.demo.browser.BrowserStartup;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.swing.BasicPanel;

import javax.swing.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

public class CopySelectionAction extends AbstractAction {

    protected BrowserStartup root;

    public CopySelectionAction(BrowserStartup root) {
        super("Copy");
        this.root = root;
    }


    public void actionPerformed(ActionEvent evt) {
        // ... collection seleciton here
        Toolkit tk = Toolkit.getDefaultToolkit();
        Clipboard clip = tk.getSystemClipboard();
        clip.setContents(new StringSelection("..."), null);
    }
}

