package se.j4j.guavaextensions;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import se.j4j.testlib.UtilityClassTester;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Tests for {@link Functions2}
 */
public class Functions2Test
{
	static final Function<Integer, Integer> ADD_ONE = new Function<Integer, Integer>(){
		@Override
		public Integer apply(Integer input)
		{
			return input + 1;
		}
	};

	@Test
	public void testRepeatThreeTimes()
	{
		assertThat(Functions2.repeat(ADD_ONE, 2).apply(2)).isEqualTo(4);
	}

	@Test
	public void testCompoundFunction()
	{
		assertThat(Functions2.compound(ADD_ONE, ADD_ONE).apply(2)).isEqualTo(4);
		Function<Integer, Integer> noOp = Functions.identity();
		assertThat(Functions2.compound(noOp, ADD_ONE).apply(2)).isEqualTo(3);
	}

	@Test
	public void testListTransformer()
	{
		List<Integer> transformedList = Functions2.listTransformer(ADD_ONE).apply(Arrays.asList(1, 2, 3));
		assertThat(transformedList).isEqualTo(Arrays.asList(2, 3, 4));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThatListTransformerReturnsImmutableList()
	{
		Functions2.listTransformer(ADD_ONE).apply(Arrays.asList(1, 2, 3)).add(4);
	}

	@Test
	public void testThatListTransformerReturnsIdentityFunctionWhenGivenIdentityFunction()
	{
		assertThat(Functions2.listTransformer(Functions.identity())).isSameAs(Functions.identity());
	}

	@Test
	public void testMapTransformer()
	{
		Map<Integer, Integer> input = new ImmutableMap.Builder<Integer, Integer>().put(1, 2).build();
		Map<Integer, Integer> output = new ImmutableMap.Builder<Integer, Integer>().put(1, 3).build();

		Map<Integer, Integer> transformedMap = Functions2.<Integer, Integer>mapValueTransformer(ADD_ONE).apply(input);
		assertThat(transformedMap).isEqualTo(output);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThatMapTransformerReturnsImmutableList()
	{
		Functions2.mapValueTransformer(ADD_ONE).apply(Maps.<Object, Integer>newHashMap()).clear();
	}

	@Test
	public void testThatMapTransformerReturnsIdentityFunctionWhenGivenIdentityFunction()
	{
		assertThat(Functions2.mapValueTransformer(Functions.identity())).isSameAs(Functions.identity());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnmodifiableListMaker()
	{
		Functions2.unmodifiableList().apply(Arrays.asList(new Object())).clear();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnmodifiableMapMaker()
	{
		Functions2.unmodifiableMap().apply(Collections.emptyMap()).clear();
	}

	@Test
	public void testUtilityClassDesign()
	{
		UtilityClassTester.testUtilityClassDesign(Functions2.class);
	}
}
