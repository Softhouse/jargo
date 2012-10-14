package se.j4j.testlib;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;

public class ResourceLoaderTest
{
	@Test
	public void testResourceLoader()
	{
		assertThat(ResourceLoader.get("/resource-loader-test.txt")).isEqualTo("some text");
	}

	@Test
	public void testResourceLoaderForNonExistingResource()
	{
		String resourceName = "foo.txt";
		try
		{
			ResourceLoader.get("foo.txt");
			fail("foo.txt should not exist");
		}
		catch(IllegalArgumentException expected)
		{
			assertThat(expected).hasMessage(String.format("resource %s relative to %s not found.", resourceName, ResourceLoader.class.getName()));
		}
	}

	@Test
	public void testNullContracts()
	{
		new NullPointerTester().testStaticMethods(ResourceLoader.class, Visibility.PACKAGE);
	}
}
