package com.benbenlaw.castingmb.data.custom;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.recipe.EntityMeltingRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EntityMeltingRecipeBuilder implements RecipeBuilder {

    protected String group;
    protected EntityType<?> entity;
    protected List<FluidStackTemplate> output;
    protected int meltingTemp;
    protected int damage;
    protected Optional<Double> durationModifier;
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public EntityMeltingRecipeBuilder(EntityType<?> entity, List<FluidStackTemplate> output, int meltingTemp, int damage, Optional<Double> durationModifier) {
        this.entity = entity;
        this.output = output;
        this.meltingTemp = meltingTemp;
        this.damage = damage;
        this.durationModifier = durationModifier;
    }

    public static EntityMeltingRecipeBuilder meltingRecipesBuilder(EntityType<?> entity, List<FluidStackTemplate> output, int meltingTemp, int damage, Optional<Double> durationModifier) {
        return new EntityMeltingRecipeBuilder(entity, output, meltingTemp, damage, durationModifier);
    }

    @Override
    public @NotNull RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public @NotNull RecipeBuilder group(@Nullable String groupName) {
        this.group = groupName;
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return ResourceKey.create(
                Registries.RECIPE,
                Casting.identifier("melting/" + entity.builtInRegistryHolder().key().identifier().getPath())
        );
    }

    @Override
    public void save(@NotNull RecipeOutput recipeOutput, @NotNull String id) {
        save(recipeOutput, ResourceKey.create(Registries.RECIPE, CastingMB.identifier("entity_melting/" + id)));
    }

    @Override
    public void save(RecipeOutput recipeOutput, @NotNull ResourceKey<Recipe<?>> resourceKey) {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(resourceKey))
                .rewards(AdvancementRewards.Builder.recipe(resourceKey))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(builder::addCriterion);
        EntityMeltingRecipe meltingRecipe = new EntityMeltingRecipe(this.entity, this.output, this.meltingTemp, this.damage, this.durationModifier);
        recipeOutput.accept(resourceKey, meltingRecipe, builder.build(resourceKey.identifier().withPrefix("recipes/entity_melting/")));

    }
}
