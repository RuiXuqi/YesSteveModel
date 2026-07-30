package com.elfmcys.ysm.format.parser.pojo.manifest.models;

import com.google.gson.annotations.SerializedName;

public class ModelFiles {
    public PlayerModelFiles player;

    @Deprecated
    public ReplacedModelFiles arrow;

    public ModelFilesWarper<ReplacedModelFiles> projectiles = new ModelFilesWarper<>();
    public ModelFilesWarper<ReplacedModelFiles> vehicles = new ModelFilesWarper<>();

    @SerializedName("sound_path")
    public String soundPath;

    @SerializedName("function_path")
    public String functionPath;

    @SerializedName("language_path")
    public String languagePath;
}
