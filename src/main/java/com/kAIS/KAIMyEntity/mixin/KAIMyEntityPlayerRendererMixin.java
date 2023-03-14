package com.kAIS.KAIMyEntity.mixin;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.renderer.IMMDModel;
import com.kAIS.KAIMyEntity.renderer.MMDAnimManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager.ModelWithPlayerData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class KAIMyEntityPlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public KAIMyEntityPlayerRendererMixin(Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(AbstractClientPlayer entityIn, float entityYaw, float partialTicks, PoseStack poseStackIn, MultiBufferSource bufferIn, int packedLightIn, CallbackInfo ci) {
        IMMDModel model = null;
        float bodyYaw = entityIn.yBodyRot;
        float bodyPitch = 0.0f;
        Vector3f entityTrans = new Vector3f(0.0f);
        MMDModelManager.Model m = MMDModelManager.GetPlayerModel("EntityPlayer_" + entityIn.getName().getString());
        if (m == null)
            m = MMDModelManager.GetPlayerModel("EntityPlayer");
        if (m == null){
            super.render(entityIn, entityYaw, partialTicks, poseStackIn, bufferIn, packedLightIn);
            return;
        } 
        if (m != null)
            model = m.model;

        MMDModelManager.ModelWithPlayerData mwpd = (MMDModelManager.ModelWithPlayerData) m;
        mwpd.loadModelProperties(KAIMyEntityClient.reloadProperties);
        float sleepingPitch = mwpd.properties.getProperty("sleepingPitch") == null ? 0.0f : Float.valueOf(mwpd.properties.getProperty("sleepingPitch"));
        Vector3f sleepingTrans = mwpd.properties.getProperty("sleepingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwpd.properties.getProperty("sleepingTrans"));
        float flyingPitch = mwpd.properties.getProperty("flyingPitch") == null ? 0.0f : Float.valueOf(mwpd.properties.getProperty("flyingPitch"));
        Vector3f flyingTrans = mwpd.properties.getProperty("flyingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwpd.properties.getProperty("flyingTrans"));
        float swimmingPitch = mwpd.properties.getProperty("swimmingPitch") == null ? 0.0f : Float.valueOf(mwpd.properties.getProperty("swimmingPitch"));
        Vector3f swimmingTrans = mwpd.properties.getProperty("swimmingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwpd.properties.getProperty("swimmingTrans"));
        float crawlingPitch = mwpd.properties.getProperty("crawlingPitch") == null ? 0.0f : Float.valueOf(mwpd.properties.getProperty("crawlingPitch"));
        Vector3f crawlingTrans = mwpd.properties.getProperty("crawlingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwpd.properties.getProperty("crawlingTrans"));

        if (mwpd != null)
            mwpd.loadModelProperties(false);
        
        if (model != null) {
            if (!mwpd.playerData.playCustomAnim) {
                //Layer 0
                if (entityIn.getHealth() == 0.0f) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Die, 0);
                } else if (entityIn.isFallFlying()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.ElytraFly, 0);
                    bodyPitch = entityIn.getXRot() + flyingPitch;
                    entityTrans = flyingTrans;
                } else if (entityIn.isSleeping()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sleep, 0);
                    bodyYaw = entityIn.getBedOrientation().toYRot() + 180.0f;
                    bodyPitch = sleepingPitch;
                    entityTrans = sleepingTrans;
                } else if (entityIn.isPassenger()) {
                    if(entityIn.getVehicle().getType() == EntityType.HORSE && (entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f)){
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.OnHorse, 0);
                        bodyYaw = entityIn.getVehicle().getYRot();
                    }else if(entityIn.getVehicle().getType() == EntityType.HORSE){
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Ride, 0);
                        bodyYaw = entityIn.getVehicle().getYRot();
                    }else{
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Ride, 0);
                    }
                } else if (entityIn.isSwimming()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Swim, 0);
                    bodyPitch = entityIn.getXRot() + swimmingPitch;
                    entityTrans = swimmingTrans;
                } else if (entityIn.onClimbable()) {
                    if(entityIn.getY() - entityIn.yOld > 0){
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.OnClimbableUp, 0);
                    }else if(entityIn.getY() - entityIn.yOld < 0){
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.OnClimbableDown, 0);
                    }else{
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.OnClimbable, 0);
                    }
                } else if (entityIn.isSprinting() && (!entityIn.isCrouching())) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sprint, 0);
                } else if (entityIn.isVisuallyCrawling()){
                    if(entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f){
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Crawl, 0);
                    }else {
                        AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.LieDown, 0);
                    }
                    bodyPitch = crawlingPitch;
                    entityTrans = crawlingTrans;
                } else if (entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Walk, 0);
                } else {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Idle, 0);
                }

                //Layer 1
                if(!entityIn.isUsingItem() && !entityIn.swinging || entityIn.isSleeping()){
                    if (mwpd.playerData.stateLayers[1] != MMDModelManager.PlayerData.EntityState.Idle) {
                        mwpd.playerData.stateLayers[1] = MMDModelManager.PlayerData.EntityState.Idle;
                        model.ChangeAnim(0, 1);
                    }
                }else{
                    if((entityIn.getUsedItemHand() == InteractionHand.MAIN_HAND) && entityIn.isUsingItem()){
                        String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.MAIN_HAND);
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.ItemRight, itemId, "Right", "using", 1);
                    }else if((entityIn.swingingArm == InteractionHand.MAIN_HAND) && entityIn.swinging){
                        String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.MAIN_HAND);
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.SwingRight, itemId, "Right", "swinging", 1);
                    }else if((entityIn.getUsedItemHand() == InteractionHand.OFF_HAND) && entityIn.isUsingItem()){
                        String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.OFF_HAND);
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.ItemLeft, itemId, "Left", "using", 1);
                    }else if((entityIn.swingingArm == InteractionHand.OFF_HAND) && entityIn.swinging){
                        String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.OFF_HAND);
                        CustomItemActiveAnim(mwpd, MMDModelManager.PlayerData.EntityState.SwingLeft, itemId, "Left", "swinging", 1);
                    }
                }

                //Layer 2
                if (entityIn.isCrouching() && !entityIn.isVisuallyCrawling()) {
                    AnimStateChangeOnce(mwpd, MMDModelManager.PlayerData.EntityState.Sneak, 2);
                } else {
                    if (mwpd.playerData.stateLayers[2] != MMDModelManager.PlayerData.EntityState.Idle) {
                        mwpd.playerData.stateLayers[2] = MMDModelManager.PlayerData.EntityState.Idle;
                        model.ChangeAnim(0, 2);
                    }
                }
            }

            float[] size = sizeOfModel(mwpd);
            if(KAIMyEntityClient.reloadProperties)
                KAIMyEntityClient.reloadProperties = false;
            if(KAIMyEntityClient.calledFrom(6).contains("inventory")){
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                PoseStack PTS_modelViewStack = RenderSystem.getModelViewStack();
                PTS_modelViewStack.translate(0.0f, 0.0f, 1000.0f);
                PTS_modelViewStack.pushPose();
                PTS_modelViewStack.scale(20.0f,20.0f, 20.0f);
                PTS_modelViewStack.scale(size[1],size[1], size[1]);
                if(Minecraft.getInstance().gameMode.getPlayerMode() != GameType.CREATIVE)
                    PTS_modelViewStack.scale(1.5f, 1.5f, 1.5f);
                Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
                Quaternionf quaternionf1 = (new Quaternionf()).rotateX(-entityIn.getXRot() * ((float)Math.PI / 180F));
                Quaternionf quaternionf2 = (new Quaternionf()).rotateY(-entityIn.yBodyRot * ((float)Math.PI / 180F));
                quaternionf.mul(quaternionf1);
                quaternionf.mul(quaternionf2);
                PTS_modelViewStack.mulPose(quaternionf);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
                model.Render(entityIn, entityYaw, 0.0f, new Vector3f(0.0f), PTS_modelViewStack, packedLightIn);
                PTS_modelViewStack.popPose();
                poseStackIn.mulPose(quaternionf2);
                poseStackIn.scale(size[1], size[1], size[1]);
                poseStackIn.scale(0.09f, 0.09f, 0.09f);
            }else{
                poseStackIn.scale(size[0], size[0], size[0]);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
                model.Render(entityIn, bodyYaw, bodyPitch, entityTrans, poseStackIn, packedLightIn);
            }
            NativeFunc nf = NativeFunc.GetInst();
            float rotationDegree = 0.0f;
            nf.GetRightHandMat(model.GetModelLong(), mwpd.playerData.rightHandMat);
            poseStackIn.pushPose();
            poseStackIn.last().pose().mul(DataToMat(nf, mwpd.playerData.rightHandMat));
            rotationDegree = ItemRotaionDegree(entityIn, mwpd, InteractionHand.MAIN_HAND, "z");
            poseStackIn.mulPose(new Quaternionf().rotateZ(rotationDegree*((float)Math.PI / 180F)));
            rotationDegree = ItemRotaionDegree(entityIn, mwpd, InteractionHand.MAIN_HAND, "x");
            poseStackIn.mulPose(new Quaternionf().rotateX(rotationDegree*((float)Math.PI / 180F)));
            poseStackIn.scale(10.0f, 10.0f, 10.0f); 
            Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getMainHandItem(), ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, false, poseStackIn, bufferIn, entityIn.level, packedLightIn, OverlayTexture.NO_OVERLAY, 0);
            poseStackIn.popPose();

            nf.GetLeftHandMat(model.GetModelLong(), mwpd.playerData.leftHandMat);
            poseStackIn.pushPose();
            poseStackIn.last().pose().mul(DataToMat(nf, mwpd.playerData.leftHandMat));
            rotationDegree = ItemRotaionDegree(entityIn, mwpd, InteractionHand.OFF_HAND, "z");
            poseStackIn.mulPose(new Quaternionf().rotateZ(rotationDegree*((float)Math.PI / 180F)));
            rotationDegree = ItemRotaionDegree(entityIn, mwpd, InteractionHand.OFF_HAND, "x");
            poseStackIn.mulPose(new Quaternionf().rotateX(rotationDegree*((float)Math.PI / 180F)));
            poseStackIn.scale(10.0f, 10.0f, 10.0f);
            Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getOffhandItem(), ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, true, poseStackIn, bufferIn, entityIn.level, packedLightIn, OverlayTexture.NO_OVERLAY, 0);
            poseStackIn.popPose();
        }
        ci.cancel();//Added By FMyuchuan. | 隐藏模型脚下的史蒂夫
    }

    String getItemId_in_ActiveHand(AbstractClientPlayer entityIn, InteractionHand hand) {
        String descriptionId = entityIn.getItemInHand(hand).getItem().getDescriptionId();
        String result = descriptionId.substring(descriptionId.indexOf(".") + 1);
        return result;
    }

    void AnimStateChangeOnce(MMDModelManager.ModelWithPlayerData model, MMDModelManager.PlayerData.EntityState targetState, Integer layer) {
        String Property = MMDModelManager.PlayerData.stateProperty.get(targetState);
        if (model.playerData.stateLayers[layer] != targetState) {
            model.playerData.stateLayers[layer] = targetState;
            model.model.ChangeAnim(MMDAnimManager.GetAnimModel(model.model, Property), layer);
        }
    }

    void CustomItemActiveAnim(MMDModelManager.ModelWithPlayerData model, MMDModelManager.PlayerData.EntityState targetState, String itemName, String activeHand, String handState, Integer layer) {
        long anim = MMDAnimManager.GetAnimModel(model.model, String.format("itemActive_%s_%s_%s", itemName, activeHand, handState));
        if (anim != 0) {
            if (model.playerData.stateLayers[layer] != targetState) {
                model.playerData.stateLayers[layer] = targetState;
                model.model.ChangeAnim(anim, layer);
            }
            return;
        }
        if (targetState == MMDModelManager.PlayerData.EntityState.ItemRight || targetState == MMDModelManager.PlayerData.EntityState.SwingRight) {
            AnimStateChangeOnce(model, MMDModelManager.PlayerData.EntityState.SwingRight, layer);
        } else if (targetState == MMDModelManager.PlayerData.EntityState.ItemLeft || targetState == MMDModelManager.PlayerData.EntityState.SwingLeft) {
            AnimStateChangeOnce(model, MMDModelManager.PlayerData.EntityState.SwingLeft, layer);
        }
    }
    
    float DataToFloat(NativeFunc nf, long data, long pos)
    {
        int temp = 0;
        temp |= nf.ReadByte(data, pos) & 0xff;
        temp |= (nf.ReadByte(data, pos + 1) & 0xff) << 8;
        temp |= (nf.ReadByte(data, pos + 2) & 0xff) << 16;
        temp |= (nf.ReadByte(data, pos + 3) & 0xff) << 24;
        return Float.intBitsToFloat(temp);
    }
    Matrix4f DataToMat(NativeFunc nf, long data)
    {
        Matrix4f result = new Matrix4f(
            DataToFloat(nf, data, 0),DataToFloat(nf, data, 16),DataToFloat(nf, data, 32),DataToFloat(nf, data, 48),
            DataToFloat(nf, data, 4),DataToFloat(nf, data, 20),DataToFloat(nf, data, 36),DataToFloat(nf, data, 52),
            DataToFloat(nf, data, 8),DataToFloat(nf, data, 24),DataToFloat(nf, data, 40),DataToFloat(nf, data, 56),
            DataToFloat(nf, data, 12),DataToFloat(nf, data, 28),DataToFloat(nf, data, 44),DataToFloat(nf, data, 60)
        );
        result.transpose();
        return result;
    }

    float ItemRotaionDegree(AbstractClientPlayer entityIn, ModelWithPlayerData mwpd, InteractionHand iHand, String axis){
        float result = 0.0f;
        String itemId;
        String strHand;
        String handState;

        itemId = getItemId_in_ActiveHand(entityIn,iHand);

        if (iHand == InteractionHand.MAIN_HAND){
            strHand = "Right";
        } else {
            strHand = "Left";
        }

        if ((iHand == entityIn.getUsedItemHand()) && (entityIn.isUsingItem())){
            handState = "using";
        } else if ((iHand == entityIn.swingingArm) && (entityIn.swinging)){
            handState = "swinging";
        } else {
            handState = "idle";
        }

        if (mwpd.properties.getProperty(itemId + "_" + strHand + "_" + handState + "_" + axis) != null ){
            result = Float.valueOf(mwpd.properties.getProperty(itemId + "_" + strHand + "_" + handState + "_" + axis));
        } else if (mwpd.properties.getProperty("default_" + axis) != null){
            result = Float.valueOf(mwpd.properties.getProperty("default_" + axis));
        }
        
        return result;
    }

    float[] sizeOfModel(ModelWithPlayerData mwpd){
        float[] size = new float[2];
        size[0] = (mwpd.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(mwpd.properties.getProperty("size"));
        size[1] = (mwpd.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(mwpd.properties.getProperty("size_in_inventory"));
        return size;
    }
}