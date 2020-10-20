package com.ferreusveritas.dynamictrees.seasons;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.ferreusveritas.dynamictrees.api.seasons.ISeasonGrowthCalculator;
import com.ferreusveritas.dynamictrees.api.seasons.ISeasonManager;

import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SeasonManager implements ISeasonManager {
	
	private Map<Integer, SeasonContext> seasonContextMap = new HashMap<>();
	private Function<World, Tuple<ISeasonProvider, ISeasonGrowthCalculator> > seasonMapper = w -> new Tuple(new SeasonProviderNull(), new SeasonGrowthCalculatorNull());
	
	public SeasonManager() {}
	
	public SeasonManager(Function<World, Tuple<ISeasonProvider, ISeasonGrowthCalculator> > seasonMapper) {
		this.seasonMapper = seasonMapper;
	}
	
	private Tuple<ISeasonProvider, ISeasonGrowthCalculator> createProvider(World world) {
		return seasonMapper.apply(world);
	}
	
	private SeasonContext getContext(World world) {
		return seasonContextMap.computeIfAbsent(world.provider.getDimension(), d -> {
			Tuple<ISeasonProvider, ISeasonGrowthCalculator> tuple = createProvider(world);
			return new SeasonContext(tuple.getFirst(), tuple.getSecond());	
		});
	}
	
	public void setProvider(World world, ISeasonProvider provider, ISeasonGrowthCalculator calc) {
		setProvider(world, provider, calc);
	}
	
	public void flushMappings() {
		seasonContextMap.clear();
	}
	
	
	////////////////////////////////////////////////////////////////
	// Tropical Predicate
	////////////////////////////////////////////////////////////////
	
	static private final float TROPICAL_THRESHHOLD = 0.8f; //Same threshold used by Serene Seasons.  Seems smart enough 
	
	private BiPredicate<World, BlockPos> isTropical = (world, rootPos) -> world.getBiome(rootPos).getDefaultTemperature() > TROPICAL_THRESHHOLD;
	
	/**
	 * Set the global predicate that determines if a world location is tropical.
	 * Predicate should return true if tropical, false if temperate.
	 */
	public void setTropicalPredicate(BiPredicate<World, BlockPos> predicate) {
		isTropical = predicate;
	}
	
	public boolean isTropical(World world, BlockPos rootPos) {
		return isTropical.test(world, rootPos);
	}
	
	
	////////////////////////////////////////////////////////////////
	// ISeasonManager Interface
	////////////////////////////////////////////////////////////////
	
	public void updateTick(World world, long worldTicks) {
		getContext(world).updateTick(world, worldTicks);
	}
	
	public float getGrowthFactor (World world, BlockPos rootPos, float offset) {
		SeasonContext context = getContext(world);
		return isTropical(world, rootPos) ? context.getTropicalGrowthFactor(offset) : context.getTemperateGrowthFactor(offset);
	}
	
	public float getSeedDropFactor(World world, BlockPos rootPos, float offset) {
		SeasonContext context = getContext(world);
		return isTropical(world, rootPos) ? context.getTropicalSeedDropFactor(offset) : context.getTemperateSeedDropFactor(offset);
	}
	
	@Override
	public float getFruitProductionFactor(World world, BlockPos rootPos, float offset) {
		SeasonContext context = getContext(world);
		return isTropical(world, rootPos) ? context.getTropicalFruitProductionFactor(offset) : context.getTemperateFruitProductionFactor(offset);
	}
	
	public Float getSeasonValue(World world) {
		return getContext(world).getSeasonProvider().getSeasonValue(world);
	}

	@Override
	public boolean shouldSnowMelt(World world, BlockPos pos) {
		return getContext(world).getSeasonProvider().shouldSnowMelt(world, pos);
	}
	
}
