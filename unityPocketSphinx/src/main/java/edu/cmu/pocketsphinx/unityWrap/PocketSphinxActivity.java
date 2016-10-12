/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.unityWrap;


import com.unity3d.player.UnityPlayer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class PocketSphinxActivity implements RecognitionListener {

    // Unity Related methods and gameObject names
    private static final String CUnityGameObjName = "EM_UnityPocketSphinx";
    private static final String COnPartialResult = "PSOnPartialResult";
    private static final String COnResult = "PSOnResult";
    private static final String COnBeginningOfSpeech = "PSOnBeginningOfSpeech";
    private static final String COnEndOfSpeech = "PSOnEndOfSpeech";
    private static final String COnError = "PSOnError";
    private static final String COnTimeout = "PSOnTimeout";
    private static final String COnInitializeSucces = "PSOnInitializeSuccess";
    private static final String COnInitializeFailed = "PSOnInitializeFailed";
    private static final String COnPocketSphinxError = "PSOnPocketSphinxError";


    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";

    private static PocketSphinxActivity instance;
    private SpeechRecognizer recognizer;

    private String acousticModelDir = "";
    private String dictionaryPath = "";
    private float keywordThreshold = 0.0f;

    private HashMap<String, Boolean> recognizerBooleans;
    private HashMap<String, String> grammarPaths;
    private HashMap<String, String> nGramPaths;
    private HashMap<String, String> phonePaths;

    private Context activityContext;

    /*************************************************************************************
     * SpeechRecognizer Listeners
     */

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        UnityPlayer.UnitySendMessage(CUnityGameObjName, COnPartialResult, text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            UnityPlayer.UnitySendMessage(CUnityGameObjName, COnResult, text);
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        UnityPlayer.UnitySendMessage(CUnityGameObjName, COnBeginningOfSpeech, "Started recognizing...");
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        UnityPlayer.UnitySendMessage(CUnityGameObjName, COnEndOfSpeech, "Finished recognizing...");
    }


    @Override
    public void onError(Exception error) {
        UnityPlayer.UnitySendMessage(CUnityGameObjName, COnError, error.getMessage());
    }

    @Override
    public void onTimeout()
    {
        UnityPlayer.UnitySendMessage(CUnityGameObjName, COnTimeout, "Listening Timed out.");
    }

    /***************************************************************************************
     * Internal bridge methods
     */

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        SpeechRecognizerSetup recSetup = SpeechRecognizerSetup.defaultSetup();
        recSetup.setAcousticModel(new File(assetsDir, acousticModelDir));
        recSetup.setDictionary(new File(assetsDir, dictionaryPath));
        recSetup.setKeywordThreshold(keywordThreshold);
        if (!isHashMapEmptyOrNull(recognizerBooleans))
        {
            String[] keys = recognizerBooleans.keySet().toArray(new String[recognizerBooleans.size()]);
            for (int i = 0; i < keys.length; i++)
            {
                recSetup.setBoolean(keys[i], recognizerBooleans.get(keys[i]));
            }
        }

        recognizer = recSetup.getRecognizer();
        recognizer.addListener(this);


        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        if (!isHashMapEmptyOrNull(grammarPaths))
        {
            String[] keys = (String[]) grammarPaths.keySet().toArray(new String[grammarPaths.size()]);
            for (int i = 0; i < keys.length; i++)
            {
                recognizer.addGrammarSearch(keys[i], new File (assetsDir, grammarPaths.get(keys[i])));
            }
        }

        if (!isHashMapEmptyOrNull(nGramPaths))
        {
            String[] keys = (String[]) nGramPaths.keySet().toArray(new String[nGramPaths.size()]);
            for (int i = 0; i < keys.length; i++)
            {
                recognizer.addNgramSearch(keys[i], new File (assetsDir, nGramPaths.get(keys[i])));
            }
        }

        if (!isHashMapEmptyOrNull(phonePaths))
        {
            String[] keys = (String[]) phonePaths.keySet().toArray(new String[phonePaths.size()]);
            for (int i = 0; i < keys.length; i++)
            {
                recognizer.addAllphoneSearch(keys[i], new File(assetsDir, phonePaths.get(keys[i])));
            }
        }
    }

    private boolean isHashMapEmptyOrNull(HashMap map)
    {
        if (map != null)
        {
            if (!map.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    private boolean isRecognizerInitialized()
    {
        if (recognizer == null)
        {
            UnityPlayer.UnitySendMessage(CUnityGameObjName, COnPocketSphinxError, "Recognizer is not initialized!");
            return false;
        }

        return true;
    }

    /**************************************************************************************
     * External bridge methods
     */

    /**
     * Adds an boolean argument option for PocketSphinx
     * @param arg additional boolean argument
     * @param enabled boolean argument state
     */
    public void AddBoolean(String arg, boolean enabled) {
        if (recognizerBooleans == null) {
            recognizerBooleans = new HashMap<>();
        }

        recognizerBooleans.put(arg, enabled);
    }

    public void SetAcousticModelPath(String dirPath)
    {
        acousticModelDir = dirPath;
    }

    public void SetDictionaryPath(String filePath)
    {
        dictionaryPath = filePath;
    }

    public void SetKeywordThreshold(float thres)
    {
        keywordThreshold = thres;
    }

    public void AddGrammarSearchPath(String searchKey, String grammarPath)
    {
        if (grammarPaths == null)
        {
            grammarPaths = new HashMap<>();
        }

        grammarPaths.put(searchKey, grammarPath);
    }

    public void AddNGramSearchPath(String searchKey, String nGramPath)
    {
        if (nGramPaths == null)
        {
            nGramPaths = new HashMap<>();
        }

        nGramPaths.put(searchKey, nGramPath);
    }

    public void AddAllPhoneSearchPath(String searchKey, String phonePath)
    {
        if (phonePaths == null)
        {
            phonePaths = new HashMap<>();
        }

        phonePaths.put(searchKey, phonePath);
    }

    /**
     * Call this method only when the recognizer is already initialized
     * @param searchName Listens only for the keyphrase with this ID
     * @param keyphrase keyphrase listened for
     */
    public void AddKeyphraseSearch(String searchName, String keyphrase)
    {
        if (isRecognizerInitialized())
        {
            recognizer.addKeyphraseSearch(searchName, keyphrase);
        }
    }

    /**
     * Listens only what is setup in searchName
     * @param searchName
     */
    public void StartListening(String searchName)
    {
        if (isRecognizerInitialized())
        {
            recognizer.startListening(searchName);
        }
    }

    /**
     * Listens only what is setup in searchName for some time
     * @param searchName
     * @param timeout time set to listen (in milliseconds)
     */
    public void StartListening(String searchName, int timeout)
    {
        if (isRecognizerInitialized())
        {
            recognizer.startListening(searchName, timeout);
        }
    }

    /**
     * Stops speech recognition from further listening.
     * NOTE: This should be called every time OnEndOfSpeech event triggers, and
     * will give the final found hypothesis.
     */
    public void StopRecognizer()
    {
        if (isRecognizerInitialized())
        {
            recognizer.stop();
        }
    }

    /**
     * Cancels out speech recognition.
     * NOTE: Can be called anytime, but will not give out the final found hypothesis.
     */
    public void CancelRecognizer()
    {
        if (isRecognizerInitialized())
        {
            recognizer.cancel();
        }
    }

    /**
     * Destroys the current speech recognition configuration.
     * NOTE: Should be called from C# to normally deallocate everything Pocketsphinx related.
     */
    public void DestroyRecognizer()
    {
        if (isRecognizerInitialized())
        {
            recognizer.cancel();
            recognizer.removeListener(this);
            recognizer.shutdown();
            recognizer = null;
        }
    }

    public static PocketSphinxActivity getInstance()
    {
        if (instance == null)
        {
            instance = new PocketSphinxActivity();
        }

        return instance;
    }

    public void SetActivityContext(Context current)
    {
        activityContext = current;
    }


    public void RunRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(activityContext);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    UnityPlayer.UnitySendMessage(CUnityGameObjName, COnInitializeFailed, result.getMessage());
                } else {
                    UnityPlayer.UnitySendMessage(CUnityGameObjName, COnInitializeSucces, "Initialized with succces.");
                }
            }
        }.execute();

//        try
//        {
//            Assets assets = new Assets(activityContext);
//            File assetDir = assets.syncAssets();
//            setupRecognizer(assetDir);
//            UnityPlayer.UnitySendMessage(CUnityGameObjName, COnInitializeSucces, "Initialized with succces.");
//        }
//        catch (Exception result)
//        {
//            UnityPlayer.UnitySendMessage(CUnityGameObjName, COnInitializeFailed, result.getMessage());
//        }
    }
}
