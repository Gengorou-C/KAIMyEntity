package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

public class KAIMyEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    protected String modelName;
    protected Context context;

    public KAIMyEntityRenderer(Context renderManager, String entityName) {
        super(renderManager);
        this.modelName = entityName.replace(':', '.');
        this.context = renderManager;
    }

    @Override
    public boolean shouldRender(T livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntityIn, camera, camX, camY, camZ);
    }

    @Override
    public void render(T entityIn, float entityYaw, float partialTicks, PoseStack poseStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, poseStackIn, bufferIn, packedLightIn);
        String animName;
        if (entityIn.isVehicle() && (entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f)) {
            animName = "driven";
        } else if (entityIn.isVehicle()) {
            animName = "ridden";
        } else if (entityIn.isSwimming()) {
            animName = "swim";
        } else if ( (entityIn.getX() - entityIn.xOld != 0.0f || entityIn.getZ() - entityIn.zOld != 0.0f) && entityIn.getVehicle() == null) {
            animName = "walk";
        } else {
            animName = "idle";
        }
        MMDModelManager.Model model = MMDModelManager.GetNotPlayerModel(modelName, animName);
        model.loadModelProperties(false);
        float[] size = sizeOfModel(model);
        if (model != null) {
            poseStackIn.pushPose();
            if(entityIn instanceof LivingEntity)
                if(((LivingEntity) entityIn).isBaby())
                    poseStackIn.scale(0.5f, 0.5f, 0.5f);

            if(KAIMyEntityClient.calledFrom(6).contains("inventory")){
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                PoseStack PTS_modelViewStack = RenderSystem.getModelViewStack();
                PTS_modelViewStack.translate(0.0f, 0.0f, 1000.0f);
                PTS_modelViewStack.pushPose();
                PTS_modelViewStack.scale(20.0f,20.0f, 20.0f);
                PTS_modelViewStack.scale(size[1], size[1], size[1]);
                Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0f);
                Quaternion quaternion1 = Vector3f.XP.rotationDegrees(-entityIn.getXRot());
                Quaternion quaternion2 = Vector3f.YP.rotationDegrees(-entityIn.getYRot());
                quaternion.mul(quaternion1);
                quaternion.mul(quaternion2);
                PTS_modelViewStack.mulPose(quaternion);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityCutoutNoCullShader);
                model.model.Render(entityIn, entityYaw, 0.0f, new Vector3f(0.0f, 0.0f, 0.0f), PTS_modelViewStack, packedLightIn);
                PTS_modelViewStack.popPose();
            }else{
                poseStackIn.scale(size[0], size[0], size[0]);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityCutoutNoCullShader);
                model.model.Render(entityIn, entityYaw, 0.0f, new Vector3f(0.0f, 0.0f, 0.0f), poseStackIn, packedLightIn);
            }
            poseStackIn.popPose();
        }
    }

    float[] sizeOfModel(MMDModelManager.Model model){
        float[] size = new float[2];
        size[0] = (model.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size"));
        size[1] = (model.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size_in_inventory"));
        return size;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}