package com.example.moleep1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import android.os.Parcelable;
import kotlinx.parcelize.Parcelize;

@Parcelize
public class list_item extends AppCompatActivity implements Serializable {
    public String name;
    public String desc;
    public String imageuri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item);
    }

    public list_item(String name, String desc, String imageuri) {
        this.name = name;
        this.desc = desc;
        this.imageuri=imageuri;
    }

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
