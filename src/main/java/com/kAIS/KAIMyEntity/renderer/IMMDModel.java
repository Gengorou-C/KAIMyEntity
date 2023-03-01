package com.kAIS.KAIMyEntity.renderer;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.Entity;

public interface IMMDModel {
    void Render(Entity entityIn, float entityYaw, float entityPitch, Vector3f entityTrans, PoseStack mat, int packedLight);

    void ChangeAnim(long anim, long layer);

    void ResetPhysics();

    long GetModelLong();

    String GetModelDir();
}