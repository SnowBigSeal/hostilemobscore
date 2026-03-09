package com.snowbigdeal.hostilemobscore.mixin;

import com.snowbigdeal.hostilemobscore.entity.slimes.BaseSlime;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class LeafCollisionMixin {

    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hostilemobscore$leafPhaseForSlime(BlockGetter level, BlockPos pos, CollisionContext context,
                                                    CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext ecc)) return;
        if (!(ecc.getEntity() instanceof BaseSlime<?>)) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (self.is(BlockTags.LEAVES)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
