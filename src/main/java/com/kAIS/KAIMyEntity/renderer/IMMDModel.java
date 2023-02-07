package com.kAIS.KAIMyEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.Entity;

public interface IMMDModel {
    void Render(Entity entityIn, float entityYaw, float entityPitch, PoseStack mat, int packedLight);

    void ChangeAnim(long anim, long layer);

    void ResetPhysics();

    long GetModelLong();

    String GetModelDir();
}