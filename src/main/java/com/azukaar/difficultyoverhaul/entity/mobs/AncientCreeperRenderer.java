package com.azukaar.difficultyoverhaul.entity.mobs;

import com.azukaar.difficultyoverhaul.DifficultyOverhaul;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.entity.MobRenderer;

@OnlyIn(Dist.CLIENT)
public class AncientCreeperRenderer extends MobRenderer<Creeper, CreeperModel<Creeper>> {
    public AncientCreeperRenderer(EntityRendererProvider.Context p_173958_) {
        super(p_173958_, new CreeperModel(p_173958_.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new CreeperPowerLayer(this, p_173958_.getModelSet()));
    }

    @Override
    protected void scale(Creeper pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        float f = pLivingEntity.getSwelling(pPartialTickTime);
        float f1 = 1.0F + Mth.sin(f * 80.0F) * f * 0.07F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        pPoseStack.scale(f2* 1.15f, f3* 1.15f, f2* 1.15f);
    }
    
    @Override
    protected float getWhiteOverlayProgress(Creeper pLivingEntity, float pPartialTicks) {
        float f = pLivingEntity.getSwelling(pPartialTicks);
        return (int) (f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
    }

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DifficultyOverhaul.MODID,
            "textures/entity/ancient_creeper.png");

    @Override
    public ResourceLocation getTextureLocation(Creeper entity) {
        return TEXTURE;
    }
}