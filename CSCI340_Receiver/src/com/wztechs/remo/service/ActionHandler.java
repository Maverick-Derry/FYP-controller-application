package com.wztechs.remo.service;


import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class ActionHandler implements ActionType{

    private final static int TYPE = 0;
    private final static String SEPARATOR = "&";
    //awt robot use to perform mouse click, move, scroll, and key typing
    private Robot robot;

    public ActionHandler(){
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }


    void performAction(String actionCode, ActionCallback cb){
        if(actionCode == null) return;

        try {
            String[] args = actionCode.split(SEPARATOR);
            //start to perform all type of actions
            switch (args[TYPE]) {
                case KEY_INSERT:
                    if (args[1].equals("0")) {
                        robot.keyPress(KeyEvent.VK_NUMPAD0 );
                        robot.keyRelease(KeyEvent.VK_NUMPAD0 );
                    }
                    else if (args[1].equals("1")) {
                        robot.keyPress(KeyEvent.VK_NUMPAD1);
                        robot.keyRelease(KeyEvent.VK_NUMPAD1);
                    }
                    else if (args[1].equals("2")) {
                        robot.keyPress(KeyEvent.VK_NUMPAD2);
                        robot.keyRelease(KeyEvent.VK_NUMPAD2);
                    }
                    else if (args[1].equals("3")) {
                        robot.keyPress(KeyEvent.VK_NUMPAD3);
                        robot.keyRelease(KeyEvent.VK_NUMPAD3);
                    }
                    break;
                case SYSTEM_QUIT:
                    System.exit(0);
                    break;
                default:
                    // System.out.println(args[TYPE]);
            }
        }
        catch(IndexOutOfBoundsException | NumberFormatException e){
            e.printStackTrace();
        }
    }

    private void setClipboardContents(String string){
        StringSelection stringSelection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
                (contents != null) &&
                        contents.isDataFlavorSupported(DataFlavor.stringFlavor)
                ;
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException ex){
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        if(result.length() > 400)
            return result.substring(0, 400);
        else
            return result;
    }
}

