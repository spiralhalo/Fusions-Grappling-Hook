package com.fusionflux.grapple.items;

import com.fusionflux.grapple.Grapple;
import com.fusionflux.grapple.client.sound.GrappleSoundInstance;
import com.fusionflux.grapple.entity.HookPoint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.render.entity.LeashKnotEntityRenderer;
import net.minecraft.client.render.entity.model.LeashKnotEntityModel;
import net.minecraft.client.sound.ElytraSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class GrappleItem extends Item {

    public GrappleItem(Settings settings) {
        super(settings);
    }

    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound tag = stack.getOrCreateNbt();
        HitResult hitResult = user.raycast(128.0D, 0.0F, false);
        Vec3d hitPos;
        double distance;
        boolean isHooked = tag.getBoolean("isHooked");


        if (!isHooked) {

            //Check to make sure the HitResult hits a block
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.NEUTRAL, 1F, 1F);
                world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.NEUTRAL, 1F, 1F);
                world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.NEUTRAL, 1F, 1F);
                world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundCategory.NEUTRAL, 1F, 1F);
                world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.NEUTRAL, 1F, 1F);

                //Gets the position of the HitResult
                hitPos = hitResult.getPos();

                BlockSoundGroup blockSoundGroup = world.getBlockState(((BlockHitResult) hitResult).getBlockPos()).getSoundGroup();

                world.playSound(null, hitPos.x,hitPos.y,hitPos.z, blockSoundGroup.getBreakSound(), SoundCategory.NEUTRAL, 1F, blockSoundGroup.getPitch());
                world.syncWorldEvent(user, 2001, ((BlockHitResult) hitResult).getBlockPos(), Block.getRawIdFromState(world.getBlockState(((BlockHitResult) hitResult).getBlockPos())));


                List<Long> posXList = new ArrayList<>();
                List<Long> posYList = new ArrayList<>();
                List<Long> posZList = new ArrayList<>();


                HookPoint hookPoint;
                hookPoint = Grapple.HOOK_POINT.create(world);
                assert hookPoint != null;
                hookPoint.setPos(hitPos.x,hitPos.y,hitPos.z);
                hookPoint.setConnected(user.getUuidAsString());
                if (!world.isClient) {
                    world.spawnEntity(hookPoint);
                }
                NbtList test = stack.getOrCreateNbt().getList("entities",11);
                test.add(NbtHelper.fromUuid(hookPoint.getUuid()));
                stack.getOrCreateNbt().put("entities", test);

                posXList.add(Double.doubleToLongBits(hitPos.x));
                posYList.add(Double.doubleToLongBits(hitPos.y));
                posZList.add(Double.doubleToLongBits(hitPos.z));

                stack.getOrCreateNbt().putLongArray("xList", posXList);
                stack.getOrCreateNbt().putLongArray("yList", posYList);
                stack.getOrCreateNbt().putLongArray("zList", posZList);


                
                //Gets the distance between the HitResult Position and the Players Position
                distance = hitPos.distanceTo(user.getEyePos());
                

                stack.getOrCreateNbt().putDouble("distance", distance);

                stack.getOrCreateNbt().putBoolean("isHooked", true);
                stack.getOrCreateNbt().putBoolean("initalBoost", false);
            }
        }
        if (isHooked) {
            world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.NEUTRAL, 1F, 2F);
            world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.NEUTRAL, 1F, 2F);
            world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.NEUTRAL, 1F, 2F);
            world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundCategory.NEUTRAL, 1F, 2F);
            world.playSound(null, user.getPos().x,user.getPos().y,user.getPos().z, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.NEUTRAL, 1F, 2F);

            NbtList UUIDs = stack.getOrCreateNbt().getList("entities",11);

            for (NbtElement uuid : UUIDs) {
                if (!world.isClient) {
                    HookPoint hookPoint = (HookPoint) ((ServerWorld) world).getEntity(NbtHelper.toUuid(uuid));
                    assert hookPoint != null;
                    hookPoint.kill();
                }
            }

            NbtList clearUUIDs = new NbtList();
            stack.getOrCreateNbt().put("entities", clearUUIDs);


            stack.getOrCreateNbt().putBoolean("isHooked", false);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        NbtCompound tag = stack.getOrCreateNbt();
        boolean initalBoost = tag.getBoolean("initalBoost");
        boolean isHooked = tag.getBoolean("isHooked");
        double distance = tag.getDouble("distance");
        Vec3d hitPos = Vec3d.ZERO;

        if (isHooked){
        long[] xList = tag.getLongArray("xList");
        long[] yList = tag.getLongArray("yList");
        long[] zList = tag.getLongArray("zList");

            List<Long> posXList = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(xList)));
            List<Long> posYList = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(yList)));
            List<Long> posZList = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(zList)));

        int lastValue = posXList.size()-1;

        if(lastValue>=1){

            Vec3d startHitPos = new Vec3d(Double.longBitsToDouble(posXList.get(lastValue-1)), Double.longBitsToDouble(posYList.get(lastValue-1)), Double.longBitsToDouble(posZList.get(lastValue-1)));

            BlockHitResult hitResult = static_raycastBlock(world,entity.getEyePos(),startHitPos,entity,pos -> {if (Objects.equals(pos, new BlockPos(entity.getEyePos().x, entity.getEyePos().y, entity.getEyePos().z)) || Objects.equals(pos, new BlockPos(startHitPos.x, startHitPos.y, startHitPos.z))) return true; return false;});

            if(hitResult.getPos() == startHitPos){
                Vec3d otherPoint = new Vec3d(Double.longBitsToDouble(posXList.get(lastValue)), Double.longBitsToDouble(posYList.get(lastValue)), Double.longBitsToDouble(posZList.get(lastValue)));

                distance += otherPoint.distanceTo(startHitPos);
                stack.getOrCreateNbt().putDouble("distance", distance);

                NbtList UUIDs = stack.getOrCreateNbt().getList("entities",11);
                if (!world.isClient) {
                    HookPoint hookPoint = (HookPoint) ((ServerWorld) world).getEntity(NbtHelper.toUuid(UUIDs.get(lastValue)));
                    assert hookPoint != null;
                    hookPoint.kill();
                    hookPoint = (HookPoint) ((ServerWorld) world).getEntity(NbtHelper.toUuid(UUIDs.get(lastValue-1)));
                    assert hookPoint != null;
                    hookPoint.setConnected(entity.getUuidAsString());
                }
                UUIDs.remove(lastValue);
                stack.getOrCreateNbt().put("entities", UUIDs);
                System.out.println(stack.getOrCreateNbt().getList("entities",11));
                posXList.remove(lastValue);
                posYList.remove(lastValue);
                posZList.remove(lastValue);
                lastValue = posXList.size()-1;
            }

        }


        Vec3d startHitPos = new Vec3d(Double.longBitsToDouble(posXList.get(lastValue)), Double.longBitsToDouble(posYList.get(lastValue)), Double.longBitsToDouble(posZList.get(lastValue)));

        BlockHitResult hitResult = static_raycastBlock(world,entity.getEyePos(),startHitPos,entity,pos -> {if (Objects.equals(pos, new BlockPos(entity.getEyePos().x, entity.getEyePos().y, entity.getEyePos().z)) || Objects.equals(pos, new BlockPos(startHitPos.x, startHitPos.y, startHitPos.z))) return true; return false;});

        if(hitResult.getPos() != startHitPos){
            if(!posXList.contains(Double.doubleToLongBits(hitResult.getPos().x))) {
                posXList.add(Double.doubleToLongBits(hitResult.getPos().x));
                posYList.add(Double.doubleToLongBits(hitResult.getPos().y));
                posZList.add(Double.doubleToLongBits(hitResult.getPos().z));
                distance -= hitResult.getPos().distanceTo(startHitPos);
                stack.getOrCreateNbt().putDouble("distance", distance);

                HookPoint hookPoint;
                hookPoint = Grapple.HOOK_POINT.create(world);
                assert hookPoint != null;
                hookPoint.setPos(hitResult.getPos().x,hitResult.getPos().y,hitResult.getPos().z);
                if (!world.isClient) {
                    world.spawnEntity(hookPoint);
                    NbtList UUIDs = stack.getOrCreateNbt().getList("entities",11);
                    HookPoint hookPoint2 = (HookPoint) ((ServerWorld) world).getEntity(NbtHelper.toUuid(UUIDs.get(lastValue)));
                    assert hookPoint2 != null;
                    hookPoint2.setConnected(hookPoint.getUuidAsString());
                }


                NbtList UUIDs = stack.getOrCreateNbt().getList("entities",11);
                UUIDs.add(NbtHelper.fromUuid(hookPoint.getUuid()));
                stack.getOrCreateNbt().put("entities", UUIDs);
                System.out.println(stack.getOrCreateNbt().getList("entities",11));

            }
        }
            lastValue = posXList.size()-1;

        hitPos = new Vec3d(Double.longBitsToDouble(posXList.get(lastValue)), Double.longBitsToDouble(posYList.get(lastValue)), Double.longBitsToDouble(posZList.get(lastValue)));




        if (!entity.isOnGround()) {
            //ElytraSoundInstance(entity);
            /*if(MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().getSoundManager().play(new GrappleSoundInstance(MinecraftClient.getInstance().player));
            }*/
            //entity.playSound(SoundEvents.ITEM_ELYTRA_FLYING,1,1);
            //world.playSound(null, entity.getPos().x,entity.getPos().y,entity.getPos().z, SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.NEUTRAL, 1F, 1F);
            ((PlayerEntity) entity).setNoDrag(true);
            entity.setNoGravity(true);

            Vec3d grappleVector = hitPos.subtract(entity.getEyePos());
            Vec3d horizontalCorrection = grappleVector.normalize().multiply((.08) , .08, (.08));
            Vec3d horizontalBoost = grappleVector.normalize().multiply((entity.getVelocity().y)*.6666 , .08, (entity.getVelocity().y)*.6666);
            double testy = horizontalCorrection.y;
            horizontalCorrection = horizontalCorrection.multiply(1, 0, 1);

            if (hitPos.y >= entity.getEyePos().y) {

                if (!initalBoost && hitPos.distanceTo(entity.getEyePos()) > distance) {
                    if (Math.abs(entity.getVelocity().x) < Math.abs(horizontalBoost.x)) {
                        entity.setVelocity(entity.getVelocity().add(-horizontalBoost.x, 0, 0));
                    }
                    if (Math.abs(entity.getVelocity().z) < Math.abs(horizontalBoost.z)) {
                        entity.setVelocity(entity.getVelocity().add(0, 0, -horizontalBoost.z));
                    }
                    stack.getOrCreateNbt().putBoolean("initalBoost", true);
                }

                if (hitPos.distanceTo(entity.getEyePos()) > distance) {
                    entity.setVelocity(entity.getVelocity().add(horizontalCorrection));

                }
                if (hitPos.distanceTo(entity.getEyePos()) > distance) {
                    entity.setVelocity(entity.getVelocity().add(new Vec3d(0, .08, 0)));
                }

                if (hitPos.distanceTo(entity.getEyePos()) > distance && entity.getVelocity().y < 0) {
                    entity.setVelocity((new Vec3d(entity.getVelocity().x, entity.getVelocity().y/2, entity.getVelocity().z)));
                }

                if (hitPos.distanceTo(entity.getEyePos()) < distance+.1) {
                    entity.setVelocity(entity.getVelocity().add(new Vec3d(0, -.08, 0)));
                }

            }

            if (hitPos.y < entity.getEyePos().y) {
               /* if (hitPos.distanceTo(entity.getEyePos()) > distance) {
                    if (Math.abs(entity.getVelocity().x) < Math.abs(horizontalBoost.x)) {
                        entity.setVelocity(entity.getVelocity().add(-horizontalBoost.x, 0, 0));
                    }
                    if (Math.abs(entity.getVelocity().z) < Math.abs(horizontalBoost.z)) {
                        entity.setVelocity(entity.getVelocity().add(0, 0, -horizontalBoost.z));
                    }
                }*/
                if (hitPos.distanceTo(entity.getEyePos()) > distance) {
                    entity.setVelocity(entity.getVelocity().add(horizontalCorrection));
                }
                if (hitPos.distanceTo(entity.getEyePos()) > distance && entity.getVelocity().y > 0) {
                    entity.setVelocity((new Vec3d(entity.getVelocity().x, entity.getVelocity().y/2, entity.getVelocity().z)));
                }

                //if (hitPos.distanceTo(entity.getEyePos()) < distance+.1) {
                    entity.setVelocity(entity.getVelocity().add(new Vec3d(0, -.08, 0)));
               // }
            }
        }

            entity.setVelocity(entity.getVelocity().multiply(.98));

            stack.getOrCreateNbt().putLongArray("xList", posXList);
            stack.getOrCreateNbt().putLongArray("yList", posYList);
            stack.getOrCreateNbt().putLongArray("zList", posZList);

        } else {
            ((PlayerEntity) entity).setNoDrag(false);
            entity.setNoGravity(false);
        }

        if (!isHooked) {

            List<Long> emptyList = new ArrayList<>();
            stack.getOrCreateNbt().putLongArray("xList", emptyList);
            stack.getOrCreateNbt().putLongArray("yList", emptyList);
            stack.getOrCreateNbt().putLongArray("zList", emptyList);
        }

        if (isHooked && entity.isOnGround()) {
            distance = hitPos.distanceTo(entity.getEyePos());

            stack.getOrCreateNbt().putDouble("distance", distance);
        }

    }

    public static BlockHitResult static_raycastBlock(World world, Vec3d start, Vec3d end,Entity entity, Predicate<BlockPos> shouldSkip) {
        return BlockView.raycast(start, end, new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity), (ctx, pos) -> {
            if (shouldSkip.test(pos)) return null;
            BlockState state = world.getBlockState(pos);
            VoxelShape shape = ctx.getBlockShape(state, world, pos);
            BlockHitResult hit = world.raycastBlock(start, end, pos, shape, state);
            return hit;
        }, (ctx)-> {
            Vec3d vec3d = ctx.getStart().subtract(ctx.getEnd());
            return BlockHitResult.createMissed(ctx.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(ctx.getEnd()));
        });
    }

}
