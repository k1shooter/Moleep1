package com.example.moleep1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.UUID;

import android.os.Parcelable;
import kotlinx.parcelize.Parcelize;

public class list_item implements Serializable {
    private String id;
    public String name;
    public String desc;
    public String imageuri;


    public list_item(String id, String name, String desc, String imageUri) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.imageuri = imageUri;
    }
    public list_item(String name, String desc, String imageUri) {
        this(java.util.UUID.randomUUID().toString(), name, desc, imageUri);
    }

    public String getId() { return id; }

    public String getName(){
        return this.name;
    }

    public String getDesc(){
        return this.desc;
    }

    public String getImageUri(){
        return this.imageuri;
    }

    public void setName(String name) { this.name = name; }
    public void setDesc(String desc) { this.desc = desc; }
    public void setImageUri(String imageUri) { this.imageuri = imageUri; }
    private static final long serialVersionUID = 1L;
}
