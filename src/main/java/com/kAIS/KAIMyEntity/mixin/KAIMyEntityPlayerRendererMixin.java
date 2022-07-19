package com.kAIS.KAIMyEntity.mixin;

import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.renderer.IMMDModel;
import com.kAIS.KAIMyEntity.renderer.MMDAnimManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.InteractionHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PlayerRenderer.class)
public abstract class KAIMyEntityPlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public KAIMyEntityPlayerRendererMixin(Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(AbstractClientPlayer entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CallbackInfo ci) {
        IMMDModel model = null;
        MMDModelManager.Model m = MMDModelManager.GetPlayerModel("EntityPlayer_" + entityIn.getName().getString());
        if (m == null)
            m = MMDModelManager.GetPlayerModel("EntityPlayer");
        if (m != null)
            model = m.model;

        MMDModelManager.ModelWithPlayerData mwpd = (MMDModelManager.ModelWithPlayerData) m;

        if (model != null) {
            if (!mwpd.playerData.playCustomAnim) {
                //Layer 0
                if (entityIn.getHealth() == 0.0f) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Die, 0);
                } else if (entityIn.isFallFlying()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.ElytraFly, 0);
                } else if (entityIn.isSleeping()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sleep, 0);
                } else if (entityIn.isPassenger()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Ride, 0);
                } else if (entityIn.isSwimming()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Swim, 0);
                } else if (entityIn.onClimbable()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.OnLadder, 0);
                } else if (entityIn.isSprinting() && (!entityIn.isCrouching())) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sprint, 0);
                } else if (entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Walk, 0);
                } else {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Idle, 0);
                }

                //Layer 1
                if (entityIn.isUsingItem()) {
                    if (entityIn.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.ItemRight, Objects.requireNonNull(entityIn.getUseItem().getItem().getName(entityIn.getUseItem())).toString().replace(':', '.'), false);
                    } else {
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.ItemLeft, Objects.requireNonNull(entityIn.getUseItem().getItem().getName(entityIn.getUseItem())).toString().replace(':', '.'), true);
                    }
                } else if (entityIn.swinging) {
                    if (entityIn.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.SwingRight, 1);
                    } else if (entityIn.getUsedItemHand() == InteractionHand.OFF_HAND) {
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.SwingLeft, 1);
                    }
                } else {
                    if (mwpd.playerData.stateLayers[1] != MMDModelManager.PlayerData.EntityState.Idle) {
                        mwpd.playerData.stateLayers[1] = MMDModelManager.PlayerData.EntityState.Idle;
                        model.ChangeAnim(0, 1);
                    }
                }

                //Layer 2
                if (entityIn.isCrouching()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sneak, 2);
                } else {
                    if (mwpd.playerData.stateLayers[2] != MMDModelManager.PlayerData.EntityState.Idle) {
                        mwpd.playerData.stateLayers[2] = MMDModelManager.PlayerData.EntityState.Idle;
                        model.ChangeAnim(0, 2);
                    }
                }
            }

            model.Render(entityYaw, matrixStackIn, packedLightIn);

            NativeFunc nf = NativeFunc.GetInst();
            nf.GetRightHandMat(model.GetModelLong(), mwpd.playerData.rightHandMat);
            matrixStackIn.pushPose();
            Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getMainHandItem(), ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, bufferIn, entityIn.level, packedLightIn, OverlayTexture.NO_OVERLAY, 0);
            matrixStackIn.popPose();

            nf.GetLeftHandMat(model.GetModelLong(), mwpd.playerData.leftHandMat);
            matrixStackIn.pushPose();
            Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getOffhandItem(), ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, true, matrixStackIn, bufferIn, entityIn.level, packedLightIn, OverlayTexture.NO_OVERLAY, 0);
            matrixStackIn.popPose();
        }
        ci.cancel();//Added By FMyuchuan. | 隐藏模型脚下的史蒂夫
    }

    void AnimStateChangeOnce(MMDModelManager.ModelWithPlayerData model, MMDModelManager.PlayerData.EntityState targetState, Integer layer) {
        String Property = MMDModelManager.PlayerData.stateProperty.get(targetState);
        if (model.playerData.stateLayers[layer] != targetState) {
            model.playerData.stateLayers[layer] = targetState;
            model.model.ChangeAnim(MMDAnimManager.GetAnimModel(model.model, Property), layer);
        }
    }

    void CustomItemActiveAnim(MMDModelManager.ModelWithPlayerData model, MMDModelManager.PlayerData.EntityState targetState, String itemName, boolean isLeftHand) {
        long anim = MMDAnimManager.GetAnimModel(model.model, String.format("itemActive_%s_%s", itemName, isLeftHand ? "left" : "right"));
        if (anim != 0) {
            if (model.playerData.stateLayers[1] != targetState) {
                model.playerData.stateLayers[1] = targetState;
                model.model.ChangeAnim(anim, 1);
            }
        }
    }
}
