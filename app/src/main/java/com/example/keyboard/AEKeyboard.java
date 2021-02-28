package com.example.keyboard;
import java.security.*;
import javax.crypto.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import java.util.*;

import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import androidx.annotation.RequiresApi;

public class AEKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    private  boolean isCaps = false;
    public String s="";
    //Press Ctrl+O

    String outputString;
    String AES = "AES";
    String key="";

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard,null);
        keyboard = new Keyboard(this,R.xml.qwerty);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }

    public void switchInputMethod(String id)
    {
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard,null);
        keyboard = new Keyboard(this,R.xml.qwerty);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onKey(int i, int[] ints) {

        InputConnection ic = getCurrentInputConnection();
        playClick(i);
        CharSequence temp="";

        switch (i)
        {
            case Keyboard.KEYCODE_DELETE:
                temp="";
                temp=ic.getSelectedText(1);

                if(temp!=null)
                {
                    ic.commitText("",0);
                    //ic.deleteSurroundingText(1,0);
                    break;
                }

                ic.deleteSurroundingText(1,0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                kv.invalidateAllKeys();
                break;
            case -6:                    //encode
                temp=ic.getTextBeforeCursor(1,0);
                s="";
                temp=ic.getTextBeforeCursor(1,0);
                while(temp.length()>0) {
                    ic.deleteSurroundingText(1, 0);
                    s= temp.toString() +s;
                    temp = ic.getTextBeforeCursor(1, 0);
                }
                String enc="";
                try {
                    enc = encrypt(s,key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ic.commitText(enc,1);
                s="";
                break;

            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                break;
            case -3:            //decode
                s="";
                ClipboardManager clipboardManager;
                clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData pData = clipboardManager.getPrimaryClip();
                ClipData.Item item = pData.getItemAt(0);
                String txtpaste = item.getText().toString();
                s=txtpaste;
                temp=ic.getTextBeforeCursor(1,0);
                while(temp.length()>0) {
                    ic.deleteSurroundingText(1, 0);
                    temp = ic.getTextBeforeCursor(1, 0);
                }
                String actualString="";

                try {
                    actualString=decrypt(s,key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ic.commitText(actualString,0);
                s="";

                break;
            case -7:
                clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                pData = clipboardManager.getPrimaryClip();
                item = pData.getItemAt(0);
                txtpaste = item.getText().toString();
                s=txtpaste;
                ic.commitText(txtpaste,1);
                s="";
                break;
            case -8:
                temp=ic.getTextBeforeCursor(1,0);
                if(temp.length()<=0)
                {

                    ic.commitText(key,1);
                }

                    else
                key="";
                while(temp.length()>0) {
                    ic.deleteSurroundingText(1, 0);
                    key= temp.toString() + key;
                    temp = ic.getTextBeforeCursor(1, 0);
                }
                break;

            default:
                char code = (char)i;
                if(Character.isLetter(code) && isCaps)
                    code = Character.toUpperCase(code);
                ic.commitText(String.valueOf(code),1);
        }
        //ic.commitText(s,1);

    }

    private void playClick(int i) {

        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch(i)
        {
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

  

    @Override
    public void onText(CharSequence charSequence) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private String decrypt (String outputString, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedValue = Base64.getDecoder().decode(outputString);
        byte[] decValue = c.doFinal(decodedValue);

        String decryptedValue = new String(decValue);
        return decryptedValue;

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String encrypt (String Data, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);

        c.init(Cipher.ENCRYPT_MODE, key);

        byte[] encVal = c.doFinal(Data.getBytes());

        String encryptedValue = Base64.getEncoder().encodeToString(encVal);
        return encryptedValue;
    }
    private SecretKeySpec generateKey(String password) throws Exception
    {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes("UTF-8");
        digest.update (bytes, 0, bytes.length);
        byte [] key = digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "ARS");
        return secretKeySpec;
    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }


}
