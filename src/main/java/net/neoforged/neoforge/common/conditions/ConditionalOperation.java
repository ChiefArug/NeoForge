package net.neoforged.neoforge.common.conditions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A class representing some object {@link T} that is wrapped in one or more conditions and a modification operation on the {@link T}, if the conditions succeed.
 *
 * @param <T> The type of the held object. Note that you should NEVER be querying the type of this and should only interact with it through the provided {@link DynamicOps}{@code <}{@link T}{@code >}.
 */
public abstract class ConditionalOperation<T> implements InterceptingOps.Wrapped<T, ICondition.IContext> {
    public static final String OLD_CONDITIONS_KEY = "neoforge:conditions";
    public static final String CONDITIONS_KEY = "conditions";
    public static final String VALUE_KEY = "value";
    public static final String TYPE_KEY = "neoforge:conditional_operation_type";

    public static <T> InterceptingOps<T, ICondition.IContext> getOps(RegistryOps<T> ops, Supplier<ICondition.IContext> contextSupplier) {
        return new InterceptingOps<>(ops, makeCodec(ops), contextSupplier);
    }

    public static <T> InterceptingOps<T, ICondition.IContext> getOps(DynamicOps<T> ops, RegistryOps.RegistryInfoLookup regInfo, Supplier<ICondition.IContext> contextSupplier) {
        return new InterceptingOps<>(ops, regInfo, makeCodec(ops), contextSupplier);
    }

    public static <T> InterceptingOps<T, ICondition.IContext> getOps(DynamicOps<T> ops, HolderLookup.Provider registryAccess, Supplier<ICondition.IContext> contextSupplier) {
        return new InterceptingOps<>(ops, registryAccess, makeCodec(ops), contextSupplier);
    }

    protected static <T extends ConditionalOperation<?>> App<RecordCodecBuilder.Mu<T>, List<ICondition>> conditionList() {
        return ICondition.LIST_CODEC.fieldOf(CONDITIONS_KEY).forGetter(ConditionalOperation::getConditions);
    }

    protected static <T> Codec<T> typedPassThrough(DynamicOps<T> ops) {
        return Codec.PASSTHROUGH.xmap(x -> x.cast(ops), x -> new Dynamic<>(ops, x));
    }

    private static final ConditionalOperationCodecCache BACKWARD_COMPAT_CODEC = new ConditionalOperationCodecCache(ConditionalOperation::makeBackwardCompatCodec);
    private static <T> MapCodec<? extends ConditionalOperation<T>> makeBackwardCompatCodec(DynamicOps<T> ops) {
        return MapCodec.assumeMapUnsafe(Codec.PASSTHROUGH).xmap(dyn -> {
            List<String> keysToRemove = new ArrayList<>();
            var mapish = dyn.asMapOpt().flatMap(outerStream -> {
                List<DataResult<?>> errors = new ArrayList<>();
                var newOuter = outerStream.map(outer -> Pair.of(outer.getFirst().asString(""), outer.getSecond())).map(outer -> {
                    String outerKey = outer.getFirst();
                    var outerValue = outer.getSecond().asMapOpt();
                    outerValue.map(innerStream -> {
                        innerStream.map(inner -> Pair.of(inner.getFirst().asString(""), inner.getSecond()))
                                .filter(inner -> {
                                    String innerKey = inner.getFirst();
                                    boolean hasConditions = OLD_CONDITIONS_KEY.equals(inner.getFirst());
                                    if (!hasConditions) return true;
                                    var innerValue = inner.getSecond().asList(ICondition.CODEC::parse);
                                    //noinspection deprecation
                                    ICondition.IContext context = ConditionalOps.retrieveContext().codec().decode(ops, ops.emptyMap()).map(Pair::getFirst).getOrThrow();
                                    innerValue.stream().filter(dr -> {
                                        if (dr.isError()) {
                                            errors.add(dr);
                                            return false;
                                        }
                                        return true;
                                    }).forEach(dr -> {

                                        if (dr.getOrThrow().test(context)) {

                                        }
                                    });
                                })
                                .findFirst().ifPresent(pair42 -> {
                                    keysToRemove.add(pair42.getFirst());
                                });
                        return;
                    });
                    return;
                });
                return;
            });
            mapish = mapish;
        }, Function.identity()));
    }

    private static <T> Codec<? extends ConditionalOperation<T>> makeCodec(DynamicOps<T> ops) {
        return NeoForgeRegistries.CONDITIONAL_OPERATION_CODECS.byNameCodec().<ConditionalOperation<T>>dispatch(TYPE_KEY, ConditionalOperation::getCodecCache, codecCache -> codecCache.getFromCache(ops));
    }
    /**
     * The raw ops for the type T. DO NOT expect this to be any sort of ops with extra data such as a RegistryOps.
     * It should only be used for manipulating {@link T}
     */
    protected final DynamicOps<T> ops;

    protected final List<ICondition> conditions;

    public ConditionalOperation(DynamicOps<T> ops, List<ICondition> conditions) {
        // unwrap delegating ops as we only care about the data structure manipulation methods
        // this makes the codec cache more efficient as we will have a lot less codecs in ir
        while (ops instanceof DelegatingOps<T> doo)
            ops = doo.delegate;
        this.ops = ops;
        this.conditions = conditions;
    }

    protected List<ICondition> getConditions() {
        return conditions;
    }

    protected abstract ConditionalOperationCodecCache getCodecCache();

    /**
     * Get the result if all conditions passed (evaluated to {@code true})
     */
    protected abstract T getSuccess();

    /**
     * Get the result if at least one condition did not pass (evaluated to {@code false})
     */
    protected abstract T getFail();

    @Override
    public T unwrap(ICondition.IContext context) {
        return conditions.stream().allMatch(condition -> condition.test(context)) ? getSuccess() : getFail();
    }
}
