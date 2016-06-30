package com.openhtmltopdf.layout;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

import com.openhtmltopdf.extend.FSTextTransformer;

public class TextTransformersTest {
	@Test
	public void testSimpleToUpperTransform() {
		FSTextTransformer tr = new TextUtil.DefaultToUpperTransformer(Locale.US);
		Assert.assertThat(tr.transform("this is a Test"), equalTo("THIS IS A TEST"));	
	}

	@Test
	public void testSimpleToLowerTransform() {
		FSTextTransformer tr = new TextUtil.DefaultToLowerTransformer(Locale.US);
		Assert.assertThat(tr.transform("THIS IS a TEST"), equalTo("this is a test"));	
	}
	
	@Test
	public void testSimpleToTitleTransform() {
		FSTextTransformer tr = new TextUtil.DefaultToTitleTransformer();
		Assert.assertThat(tr.transform("this iS a teST"), equalTo("This IS A TeST"));	
	}
	
	@Test
	public void testPunctuationUnchanged() {
		FSTextTransformer[] trs = new FSTextTransformer[] {
				new TextUtil.DefaultToUpperTransformer(Locale.US),
				new TextUtil.DefaultToLowerTransformer(Locale.US),
				new TextUtil.DefaultToTitleTransformer() };
		
		for (FSTextTransformer tr : trs) {
			Assert.assertThat(tr.transform("!@#$%^&&*()_-+=?/><,.~`"), equalTo("!@#$%^&&*()_-+=?/><,.~`"));
		}
	}
}
