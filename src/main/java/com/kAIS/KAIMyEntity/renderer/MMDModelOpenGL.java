package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.KAIMyEntity;
import com.kAIS.KAIMyEntity.NativeFunc;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;

import org.lwjgl.opengl.GL46C;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MMDModelOpenGL implements IMMDModel {
    static NativeFunc nf;
    static int shaderProgram;
    static int positionLocation;
    static int uv0Location;
    static int uv2Location;
    static int normalLocation;
    static int projMatLocation;
    static int modelViewLocation;
    static int sampler0Location;
    static int sampler2Location;
    static boolean isShaderInited = false;
    long model;
    String modelDir;
    int vertexCount;
    ByteBuffer posBuffer, norBuffer, uv0Buffer, uv2Buffer;
    int vertexArrayObject;
    int indexBufferObject;
    int vertexBufferObject;
    int normalBufferObject;
    int texcoordBufferObject;
    int uv2BufferObject;
    int indexElementSize;
    int indexType;
    Material[] mats;
    Material lightMapMaterial;

    MMDModelOpenGL() {

    }

    public static void InitShader() {
        //Init Shader
        ShaderProvider.Init();
        shaderProgram = ShaderProvider.getProgram();

        //Init ShaderPropLocation
        positionLocation = GL46C.glGetAttribLocation(shaderProgram, "Position");
        uv0Location = GL46C.glGetAttribLocation(shaderProgram, "UV0");
        uv2Location = GL46C.glGetAttribLocation(shaderProgram, "UV2");
        normalLocation = GL46C.glGetAttribLocation(shaderProgram, "Normal");
        projMatLocation = GL46C.glGetUniformLocation(shaderProgram, "ProjMat");
        modelViewLocation = GL46C.glGetUniformLocation(shaderProgram, "ModelViewMat");
        sampler0Location = GL46C.glGetUniformLocation(shaderProgram, "Sampler0");
        sampler2Location = GL46C.glGetUniformLocation(shaderProgram, "Sampler2");
        isShaderInited = true;
    }

    public static MMDModelOpenGL Create(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        if (!isShaderInited)
            InitShader();
        if (nf == null) nf = NativeFunc.GetInst();
        long model;
        if (isPMD)
            model = nf.LoadModelPMD(modelFilename, modelDir, layerCount);
        else
            model = nf.LoadModelPMX(modelFilename, modelDir, layerCount);
        if (model == 0) {
            KAIMyEntity.logger.info(String.format("Cannot open model: '%s'.", modelFilename));
            return null;
        }
        BufferUploader.reset();
        //Model exists,now we prepare data for OpenGL
        int vertexArrayObject = GL46C.glGenVertexArrays();
        int indexBufferObject = GL46C.glGenBuffers();
        int positionBufferObject = GL46C.glGenBuffers();
        int normalBufferObject = GL46C.glGenBuffers();
        int uv0BufferObject = GL46C.glGenBuffers();
        int uv2BufferObject = GL46C.glGenBuffers();

        int vertexCount = (int) nf.GetVertexCount(model);
        ByteBuffer posBuffer = ByteBuffer.allocateDirect(vertexCount * 12); //float * 3
        ByteBuffer norBuffer = ByteBuffer.allocateDirect(vertexCount * 12);
        ByteBuffer uv0Buffer = ByteBuffer.allocateDirect(vertexCount * 8); //float * 2
        ByteBuffer uv2Buffer = ByteBuffer.allocateDirect(vertexCount * 8); //int * 2
        uv2Buffer.order(ByteOrder.LITTLE_ENDIAN);

        GL46C.glBindVertexArray(vertexArrayObject);
        //Init indexBufferObject
        int indexElementSize = (int) nf.GetIndexElementSize(model);
        int indexCount = (int) nf.GetIndexCount(model);
        int indexSize = indexCount * indexElementSize;
        long indexData = nf.GetIndices(model);
        ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indexSize);
        for (int i = 0; i < indexSize; ++i)
            indexBuffer.put(nf.ReadByte(indexData, i));
        indexBuffer.position(0);
        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
        GL46C.glBufferData(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL46C.GL_STATIC_DRAW);

        int indexType = switch (indexElementSize) {
            case 1 -> GL46C.GL_UNSIGNED_BYTE;
            case 2 -> GL46C.GL_UNSIGNED_SHORT;
            case 4 -> GL46C.GL_UNSIGNED_INT;
            default -> 0;
        };

        //Material
        MMDModelOpenGL.Material[] mats = new MMDModelOpenGL.Material[(int) nf.GetMaterialCount(model)];
        for (int i = 0; i < mats.length; ++i) {
            mats[i] = new MMDModelOpenGL.Material();
            String texFilename = nf.GetMaterialTex(model, i);
            if (!texFilename.isEmpty()) {
                MMDTextureManager.Texture mgrTex = MMDTextureManager.GetTexture(texFilename);
                if (mgrTex != null) {
                    mats[i].tex = mgrTex.tex;
                    mats[i].hasAlpha = mgrTex.hasAlpha;
                }
            }
        }

        //lightMap
        MMDModelOpenGL.Material lightMapMaterial = new MMDModelOpenGL.Material();
        MMDTextureManager.Texture mgrTex = MMDTextureManager.GetTexture(modelDir + "/lightMap.png");
        if (mgrTex != null) {
            lightMapMaterial.tex = mgrTex.tex;
            lightMapMaterial.hasAlpha = mgrTex.hasAlpha;
        }

        MMDModelOpenGL result = new MMDModelOpenGL();
        result.model = model;
        result.modelDir = modelDir;
        result.vertexCount = vertexCount;
        result.posBuffer = posBuffer;
        result.norBuffer = norBuffer;
        result.uv0Buffer = uv0Buffer;
        result.uv2Buffer = uv2Buffer;
        result.indexBufferObject = indexBufferObject;
        result.vertexBufferObject = positionBufferObject;
        result.texcoordBufferObject = uv0BufferObject;
        result.uv2BufferObject = uv2BufferObject;
        result.normalBufferObject = normalBufferObject;
        result.vertexArrayObject = vertexArrayObject;
        result.indexElementSize = indexElementSize;
        result.indexType = indexType;
        result.mats = mats;
        result.lightMapMaterial = lightMapMaterial;
        return result;
    }

    public static void Delete(MMDModelOpenGL model) {
        nf.DeleteModel(model.model);
    }

    public void Render(Entity entityIn, float entityYaw, PoseStack mat, int packedLight) {
        Update();
        RenderModel(entityIn, entityYaw, mat);
    }

    public void ChangeAnim(long anim, long layer) {
        nf.ChangeModelAnim(model, anim, layer);
    }

    public void ResetPhysics() {
        nf.ResetModelPhysics(model);
    }

    public long GetModelLong() {
        return model;
    }

    public String GetModelDir() {
        return modelDir;
    }

    void Update() {
        nf.UpdateModel(model);
    }

    void RenderModel(Entity entityIn, float entityYaw, PoseStack deliverStack) {
        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shader = RenderSystem.getShader();

        BufferUploader.reset();
        GL46C.glBindVertexArray(vertexArrayObject);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        deliverStack.mulPose(Vector3f.YP.rotationDegrees(-entityYaw));
        deliverStack.scale(0.09f, 0.09f, 0.09f);
        shader.MODEL_VIEW_MATRIX.set(deliverStack.last().pose());
        FloatBuffer modelViewMatBuff = shader.MODEL_VIEW_MATRIX.getFloatBuffer();
        FloatBuffer projViewMatBuff = shader.PROJECTION_MATRIX.getFloatBuffer();


        GL46C.glEnableVertexAttribArray(positionLocation);
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        GL46C.glEnableVertexAttribArray(uv0Location);

        //Position
        int posAndNorSize = vertexCount * 12; //float * 3
        long posData = nf.GetPoss(model);
        nf.CopyDataToByteBuffer(posBuffer, posData, posAndNorSize);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, vertexBufferObject);
        GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, posBuffer, GL46C.GL_STATIC_DRAW);
        GL46C.glVertexAttribPointer(positionLocation, 3, GL46C.GL_FLOAT, false, 0, 0);

        //UV0
        int uv0Size = vertexCount * 8; //float * 2
        long uv0Data = nf.GetUVs(model);
        nf.CopyDataToByteBuffer(uv0Buffer, uv0Data, uv0Size);
        GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, texcoordBufferObject);
        GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv0Buffer, GL46C.GL_STATIC_DRAW);
        GL46C.glVertexAttribPointer(uv0Location, 2, GL46C.GL_FLOAT, false, 0, 0);

        //Normal
        if(normalLocation != -1){
            GL46C.glEnableVertexAttribArray(normalLocation);
            long normalData = nf.GetNormals(model);
            nf.CopyDataToByteBuffer(norBuffer, normalData, posAndNorSize);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, normalBufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, norBuffer, GL46C.GL_STATIC_DRAW);
            GL46C.glVertexAttribPointer(normalLocation, 3, GL46C.GL_FLOAT, false, 0, 0);
        }
        

        //lightMap
        if((uv2Location != -1) && (lightMapMaterial.tex != 0)){
            minecraft.level.updateSkyBrightness();
            int skyBrightness = 16 * entityIn.level.getBrightness(LightLayer.BLOCK, entityIn.blockPosition());
            int blockBrightness = Math.round((15.0f-minecraft.level.getSkyDarken()) * (entityIn.level.getBrightness(LightLayer.SKY, entityIn.blockPosition())/15.0f) * 16);
            uv2Buffer.clear();
            for(int i = 0; i < vertexCount; i++){
                uv2Buffer.putInt(skyBrightness);
                uv2Buffer.putInt(blockBrightness);
            }
            uv2Buffer.flip();

            GL46C.glEnableVertexAttribArray(uv2Location);
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject);
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv2Buffer, GL46C.GL_STATIC_DRAW);
            GL46C.glVertexAttribIPointer(uv2Location, 2, GL46C.GL_INT, 0, 0);
        }

        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
        GlStateManager._glUseProgram(shaderProgram);

        if(sampler2Location != -1){
            RenderSystem.activeTexture(GL46C.GL_TEXTURE2);
            RenderSystem.bindTexture(lightMapMaterial.tex);
            GL46C.glUniform1i(sampler2Location, 2);
            RenderSystem.activeTexture(GL46C.GL_TEXTURE0);
        }

        GL46C.glUniformMatrix4fv(modelViewLocation, false, modelViewMatBuff);
        GL46C.glUniformMatrix4fv(projMatLocation, false, projViewMatBuff);
        long subMeshCount = nf.GetSubMeshCount(model);
        for (long i = 0; i < subMeshCount; ++i) {
            int materialID = nf.GetSubMeshMaterialID(model, i);
            float alpha = nf.GetMaterialAlpha(model, materialID);
            if (alpha == 0.0f)
                continue;

            if (nf.GetMaterialBothFace(model, materialID)) {
                RenderSystem.disableCull();
            } else {
                RenderSystem.enableCull();
            }
            if (mats[materialID].tex == 0)
                Minecraft.getInstance().getEntityRenderDispatcher().textureManager.bindForSetup(TextureManager.INTENTIONAL_MISSING_TEXTURE);
            else
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, mats[materialID].tex);
            long startPos = (long) nf.GetSubMeshBeginIndex(model, i) * indexElementSize;
            int count = nf.GetSubMeshVertexCount(model, i);

            GL46C.glUniform1i(sampler0Location, 0);
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, count, indexType, startPos);
        }
        GlStateManager._glUseProgram(0);
        BufferUploader.reset();
    }

    static class Material {
        int tex;
        boolean hasAlpha;

        Material() {
            tex = 0;
            hasAlpha = false;
        }
    }
}
