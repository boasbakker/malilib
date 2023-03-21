package malilib.util.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import malilib.util.data.Identifier;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.game.wrap.RegistryUtils;

public class BlockUtils
{
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str)
    {
        int index = str.indexOf("["); // [prop=value]
        String blockName = index != -1 ? str.substring(0, index) : str;
        Identifier id = new Identifier(blockName);

        if (Registry.BLOCK.containsKey(id))
        {
            Block block = RegistryUtils.getBlockById(id);
            BlockState state = block.defaultBlockState();

            if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']')
            {
                StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();
                String propStr = str.substring(index + 1, str.length() - 1);

                for (String propAndVal : COMMA_SPLITTER.split(propStr))
                {
                    Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                    if (valIter.hasNext() == false)
                    {
                        continue;
                    }

                    Property<?> prop = stateManager.getProperty(valIter.next());

                    if (prop == null || valIter.hasNext() == false)
                    {
                        continue;
                    }

                    Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                    if (val != null)
                    {
                        state = getBlockStateWithProperty(state, prop, val);
                    }
                }
            }

            return Optional.of(state);
        }

        return Optional.empty();
    }

    /**
     * Parses the provided string into a compound tag representing the block state.<br>
     * The tag is in the format that the vanilla util class uses for reading/writing states to NBT
     * data, for example in the Chunk block state palette.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'.<br>
     * None of the values are checked for validity here, and this can be used for
     * parsing strings for states from another Minecraft version, such as 1.12 <-> 1.13+.
     */
    public static CompoundTag getBlockStateTagFromString(String stateString)
    {
        int index = stateString.indexOf("["); // [f=b]
        String blockName = index != -1 ? stateString.substring(0, index) : stateString;
        CompoundTag tag = new CompoundTag();

        NbtWrap.putString(tag, "Name", blockName);

        if (index != -1 && stateString.length() > (index + 4) && stateString.charAt(stateString.length() - 1) == ']')
        {
            CompoundTag propsTag = new CompoundTag();
            String propStr = stateString.substring(index + 1, stateString.length() - 1);

            for (String propAndVal : COMMA_SPLITTER.split(propStr))
            {
                Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                if (valIter.hasNext() == false)
                {
                    continue;
                }

                String propName = valIter.next();

                if (valIter.hasNext() == false)
                {
                    continue;
                }

                String valStr = valIter.next();

                NbtWrap.putString(propsTag, propName, valStr);
            }

            NbtWrap.putTag(tag, "Properties", propsTag);
        }

        return tag;
    }

    /**
     * Parses the input tag representing a block state, and produces a string
     * in the same format as the toString() method in the vanilla block state.
     * This string format is what the Sponge schematic format uses in the palette.
     * @return an equivalent of BlockState.toString() of the given tag representing a block state
     */
    public static String getBlockStateStringFromTag(CompoundTag stateTag)
    {
        String name = NbtWrap.getString(stateTag, "Name");

        if (NbtWrap.containsCompound(stateTag, "Properties") == false)
        {
            return name;
        }

        CompoundTag propTag = NbtWrap.getCompound(stateTag, "Properties");
        ArrayList<Pair<String, String>> props = new ArrayList<>();

        for (String key : NbtWrap.getKeys(propTag))
        {
            props.add(Pair.of(key, NbtWrap.getString(propTag, key)));
        }

        final int size = props.size();

        if (size > 0)
        {
            props.sort(Comparator.comparing(Pair::getLeft));

            StringBuilder sb = new StringBuilder();
            sb.append(name).append('[');
            Pair<String, String> pair = props.get(0);

            sb.append(pair.getLeft()).append('=').append(pair.getRight());

            for (int i = 1; i < size; ++i)
            {
                pair = props.get(i);
                sb.append(',').append(pair.getLeft()).append('=').append(pair.getRight());
            }

            sb.append(']');

            return sb.toString();
        }

        return name;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop, Comparable<?> value)
    {
        return state.setValue(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr)
    {
        return prop.getValue(valStr).orElse(null);
    }

    /**
     * Returns the first PropertyDirection property from the provided state, if any.
     * @return the first PropertyDirection, or empty() if there are no such properties
     */
    public static Optional<DirectionProperty> getFirstDirectionProperty(BlockState state)
    {
        for (Property<?> prop : state.getProperties())
        {
            if (prop instanceof DirectionProperty)
            {
                return Optional.of((DirectionProperty) prop);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the EnumFacing value of the first found PropertyDirection
     * type block state property in the given state, if any.
     * If there are no PropertyDirection properties, then empty() is returned.
     */
    public static Optional<Direction> getFirstPropertyFacingValue(BlockState state)
    {
        Optional<DirectionProperty> propOptional = getFirstDirectionProperty(state);
        return propOptional.isPresent() ? Optional.ofNullable(state.getValue(propOptional.get())) : Optional.empty();
    }

    public static List<String> getFormattedBlockStateProperties(BlockState state)
    {
        return getFormattedBlockStateProperties(state, ": ");
    }

    public static List<String> getFormattedBlockStateProperties(BlockState state, String separator)
    {
        Collection<Property<?>> properties = state.getProperties();

        if (properties.size() > 0)
        {
            List<String> lines = new ArrayList<>();

            try
            {
                for (Property<?> prop : properties)
                {
                    Comparable<?> val = state.getValue(prop);
                    String key;

                    if (prop instanceof BooleanProperty)
                    {
                        key = val.equals(Boolean.TRUE) ? "malilib.label.block_state_properties.boolean.true" :
                                                         "malilib.label.block_state_properties.boolean.false";
                    }
                    else if (prop instanceof DirectionProperty)
                    {
                        key = "malilib.label.block_state_properties.direction";
                    }
                    else if (prop instanceof EnumProperty)
                    {
                        key = "malilib.label.block_state_properties.enum";
                    }
                    else if (prop instanceof IntegerProperty)
                    {
                        key = "malilib.label.block_state_properties.integer";
                    }
                    else
                    {
                        key = "malilib.label.block_state_properties.generic";
                    }

                    lines.add(StringUtils.translate(key, prop.getName(), separator, val.toString()));
                }
            }
            catch (Exception ignore) {}

            return lines;
        }

        return Collections.emptyList();
    }


    public static List<StyledTextLine> getBlockStatePropertyStyledTextLines(BlockState state, String separator)
    {
        Collection<Property<?>> properties = state.getProperties();

        if (properties.size() > 0)
        {
            List<StyledTextLine> lines = new ArrayList<>();

            try
            {
                for (Property<?> prop : properties)
                {
                    Comparable<?> val = state.getValue(prop);
                    String key;

                    if (prop instanceof BooleanProperty)
                    {
                        key = val.equals(Boolean.TRUE) ? "malilib.label.block_state_properties.boolean.true" :
                                                         "malilib.label.block_state_properties.boolean.false";
                    }
                    else if (prop instanceof DirectionProperty)
                    {
                        key = "malilib.label.block_state_properties.direction";
                    }
                    else if (prop instanceof EnumProperty)
                    {
                        key = "malilib.label.block_state_properties.enum";
                    }
                    else if (prop instanceof IntegerProperty)
                    {
                        key = "malilib.label.block_state_properties.integer";
                    }
                    else
                    {
                        key = "malilib.label.block_state_properties.generic";
                    }

                    lines.add(StyledTextLine.translate(key, prop.getName(), separator, val.toString()));
                }
            }
            catch (Exception ignore) {}

            return lines;
        }

        return Collections.emptyList();
    }

    public static boolean isFluidBlock(BlockState state)
    {
        return state.getMaterial().isLiquid();
    }

    public static boolean isFluidSourceBlock(BlockState state)
    {
        return state.getBlock() instanceof LiquidBlock && state.getValue(LiquidBlock.LEVEL).intValue() == 0;
    }
}
