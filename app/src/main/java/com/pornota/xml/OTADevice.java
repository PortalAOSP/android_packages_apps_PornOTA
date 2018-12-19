/*
 * Copyright (C) 2018 Chandra Poerwanto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pornota.xml;

import android.os.Environment;

import com.pornota.utils.OTAUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;

public class OTADevice implements Serializable {

    private String mLatestVersion;
    private String mBuildTime;
    private String mROMURL;
    private String mROMSize;
    private String mChangelogURL;
    private String changeLog;
    private String md5;
    private Boolean alreadyDownloaded;
    private String filename;
    private String filepath;    

    OTADevice() {
        mLatestVersion = "";
        mBuildTime = "";
        mROMURL = "";
        mROMSize = "";
        mChangelogURL = "";
        alreadyDownloaded = false;
    }

    public String getLatestVersion() {
        return mLatestVersion;
    }
    void setLatestVersion(String value) {
        this.mLatestVersion = value;
    }

    public String getBuildTime() {
        return mBuildTime;
    }
    void setBuildTime(String value) {
        this.mBuildTime = value;
    }

    public String getROMURL() {
        return mROMURL;
    }
    void setROMURL(String value) {
        this.mROMURL = value;
    }
    
    public String getROMSize() {
        return mROMSize;
    }
    public void setROMSize(String value) {
        this.mROMSize = value;
    }

    public String getChangelog() {
        return changeLog;
    }
    public String getChangelogURL() {
        return mChangelogURL;
    }
    void setChangelogURL(String value) {
        this.mChangelogURL = value;
        try{
            changeLog = readStream(OTAUtils.downloadURL(value));
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public String getChecksum() {
        return md5;
    }
    void setChecksumURL(String value) {
        try{
            md5 = readStream(OTAUtils.downloadURL(value)).split(" ")[0];
            filename = mROMURL.substring(mROMURL.lastIndexOf('/') + 1);
            filepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + filename;
            OTAUtils.logInfo("Filename : " + filepath);
            File file = new File(filepath);
            String fileChecksum="";
            if(file.exists()){
                OTAUtils.logInfo("File exists. Checking integrity.");
                try {
                    String command = "md5sum \'" + filepath + "\'\n";
                    try {
                        String line;
                        Process process = Runtime.getRuntime().exec("sh");
                        OutputStream stdin = process.getOutputStream();
                        InputStream stdout = process.getInputStream();

                        stdin.write(command.getBytes());
                        stdin.flush();

                        stdin.close();
                        BufferedReader br =
                                new BufferedReader(new InputStreamReader(stdout));
                        while ((line = br.readLine()) != null) {
                            fileChecksum = line.split(" ")[0];
                        }
                        br.close();

                        process.waitFor();
                        process.destroy();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    OTAUtils.logInfo("Checksum from server\t: " + md5);
                    OTAUtils.logInfo("Checksum of file\t: " + fileChecksum);
                    if(md5.equals(fileChecksum)){
                        alreadyDownloaded = true;
                        OTAUtils.logInfo("MD5 Matched.");
                    }else{
                        OTAUtils.logInfo("MD5 Not Matching.");}
                }
                catch (Exception e) {e.printStackTrace();}
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public String getFilename() {return filename;}
    public String getFilepath() {return filepath;}

    public Boolean isDownloadedAlready() {
        return alreadyDownloaded;
    }

    private String readStream(InputStream inputStream){
        String re="";
        try{
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            re=total.toString();
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return re;
    }

}
