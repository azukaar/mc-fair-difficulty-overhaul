package com.azukaar.difficultyoverhaul.entity.mobs;


import com.azukaar.difficultyoverhaul.DifficultyOverhaul;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.entity.RenderLayerParent;

@OnlyIn(Dist.CLIENT)
public class RaisedZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieModel<Zombie>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DifficultyOverhaul.MODID, "textures/entity/raised_zombies.png");
    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(DifficultyOverhaul.MODID, "textures/entity/raised_zombies_e.png");

    public RaisedZombieRenderer(EntityRendererProvider.Context context) {
      this(context, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);   
      this.addLayer(new RaisedZombieEyesLayer<>(this));
    }

    public RaisedZombieRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation pZombieLayer, ModelLayerLocation pInnerArmor, ModelLayerLocation pOuterArmor) {
        super(pContext, new ZombieModel(pContext.bakeLayer(pZombieLayer)), new ZombieModel(pContext.bakeLayer(pInnerArmor)), new ZombieModel(pContext.bakeLayer(pOuterArmor)));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(Zombie pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        pPoseStack.scale(1.15F, 1.15F, 1.15F);
    }

    public class RaisedZombieEyesLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
        private static final RenderType RAISED_ZOMBIE_EYES = RenderType.eyes(EMISSIVE_TEXTURE);

        public RaisedZombieEyesLayer(RenderLayerParent<T, M> renderLayerParent) {
            super(renderLayerParent);
        }

        public RenderType renderType() {
            return RAISED_ZOMBIE_EYES;
        }
    }
}