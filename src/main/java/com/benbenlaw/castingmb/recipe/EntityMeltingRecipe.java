package com.benbenlaw.castingmb.recipe;

import com.benbenlaw.core.recipe.NoInventoryRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public record EntityMeltingRecipe(EntityType<?> entity, List<FluidStackTemplate> output, int meltingTemp, int damage, Optional<Double> durationModifier) implements Recipe<NoInventoryRecipe> {

    public static final MapCodec<EntityMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    EntityType.CODEC.fieldOf("entity").forGetter(EntityMeltingRecipe::entity),
                    FluidStackTemplate.CODEC.listOf().fieldOf("output").forGetter(EntityMeltingRecipe::output),
                    Codec.INT.fieldOf("melting_temp").forGetter(EntityMeltingRecipe::meltingTemp),
                    Codec.INT.fieldOf("damage").forGetter(EntityMeltingRecipe::damage),
                    Codec.DOUBLE.optionalFieldOf("duration_modifier").forGetter(EntityMeltingRecipe::durationModifier)
            ).apply(instance, EntityMeltingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityMeltingRecipe> STREAM_CODEC = StreamCodec.of(
            EntityMeltingRecipe::write, EntityMeltingRecipe::read);

    public static final RecipeType<EntityMeltingRecipe> TYPE = new RecipeType<>() {};

    public static final RecipeSerializer<EntityMeltingRecipe> SERIALIZER =
            new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private static EntityMeltingRecipe read(RegistryFriendlyByteBuf buffer) {
        EntityType<?> entity = EntityType.STREAM_CODEC.decode(buffer);
        List<FluidStackTemplate> output = FluidStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
        int meltingTemp = buffer.readInt();
        int damage = buffer.readInt();
        Optional<Double> durationModifier = buffer.readBoolean() ? Optional.of(buffer.readDouble()) : Optional.empty();
        return new EntityMeltingRecipe(entity, output, meltingTemp, damage, durationModifier);
    }

    private static void write(RegistryFriendlyByteBuf buffer, EntityMeltingRecipe recipe) {
        EntityType.STREAM_CODEC.encode(buffer, recipe.entity);
        FluidStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.output);
        buffer.writeInt(recipe.meltingTemp);
        buffer.writeInt(recipe.damage);
        if (recipe.durationModifier.isPresent()) {
            buffer.writeBoolean(true);
            buffer.writeDouble(recipe.durationModifier.get());
        } else {
            buffer.writeBoolean(false);
        }
    }
    @Override
    public boolean matches(@NotNull NoInventoryRecipe container, @NotNull Level level) {
        return true;
    }

    //Boiler Plate
    @Override
    public @NonNull ItemStack assemble(NoInventoryRecipe recipeInput) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<? extends Recipe<NoInventoryRecipe>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<? extends Recipe<NoInventoryRecipe>> getType() {
        return TYPE;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NonNull String group() {
        return "";
    }
}
