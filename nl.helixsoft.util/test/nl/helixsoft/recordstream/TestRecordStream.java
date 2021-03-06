package nl.helixsoft.recordstream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.helixsoft.recordstream.Reducer.Count;
import nl.helixsoft.recordstream.Reducer.GroupFunc;

import junit.framework.TestCase;

public class TestRecordStream extends TestCase {

	private static class MockRecordStream implements RecordStream
	{
		private final List<String> cols;
		private final Object[][] data;
		
		MockRecordStream (String[] _cols, Object[][] data)
		{
			this.cols = Arrays.asList(_cols);
			this.data = data;
		}
		
		int row = 0;

		@Override
		public int getNumCols() 
		{
			return cols.size();
		}

		@Override
		public String getColumnName(int i) {
			return cols.get(i);
		}

		@Override
		public Record getNext() throws RecordStreamException {
			if (row >= data.length) return null;
			return new DefaultRecord(this, data[row++]);
		}

		@Override
		public int getColumnIndex(String name) 
		{
			return cols.indexOf(name);
		}
	}
	
	public void testReduce() throws RecordStreamException
	{
		Map<String, GroupFunc> map = new HashMap<String, GroupFunc>();
		map.put ("count", new Count());
		map.put ("avg_float", new Reducer.AverageFloat("float"));
		map.put ("concat_str", new Reducer.Concatenate("str", "-"));

		RecordStream rs = new MockRecordStream (
				new String[] { "groupid", "float", "str" },
				new Object[][] 	{
							{ 1, 1.0f, "abc" },
							{ 1, 2.0f, "def" },
							{ 2, 3.0f, "ghi" }, 
							{ 3, 4.0f, "jkl" }
				});

		Reducer reducer = new Reducer (rs, "groupid", map);
		
		assertEquals (4, reducer.getNumCols());
		
		Map<String, Integer> idx = new HashMap<String, Integer>();
		for (int i = 0; i < reducer.getNumCols(); ++i) idx.put (reducer.getColumnName(i), i);
		assertTrue (idx.containsKey("groupid"));
		assertTrue (idx.containsKey("count"));
		assertTrue (idx.containsKey("avg_float"));
		assertTrue (idx.containsKey("concat_str"));
		
		Record r;
		r = reducer.getNext();
		assertEquals (1, r.getValue(idx.get("groupid")));
		assertEquals (2, r.getValue(idx.get("count")));
		assertEquals (1.5f, (Float)r.getValue(idx.get("avg_float")), 0.01f);
		assertEquals ("abc-def", r.getValue(idx.get("concat_str")));
		
		r = reducer.getNext();
		assertEquals (2, r.getValue(idx.get("groupid")));
		assertEquals (1, r.getValue(idx.get("count")));
		assertEquals (3.0f, (Float)r.getValue(idx.get("avg_float")), 0.01f);
		assertEquals ("ghi", r.getValue(idx.get("concat_str")));

		r = reducer.getNext();
		assertEquals (3, r.getValue(idx.get("groupid")));
		assertEquals (1, r.getValue(idx.get("count")));
		assertEquals (4.0f, (Float)r.getValue(idx.get("avg_float")), 0.01f);
		assertEquals ("jkl", r.getValue(idx.get("concat_str")));

		r = reducer.getNext();
		assertNull (r);
	}

	public void testCast() throws RecordStreamException
	{
		RecordStream rs = new MockRecordStream (
				new String[] { "group", "col", "var" },
				new Object[][] 	{
							{ "group1", "col1", "abc" },
							{ "group1", "col2", "def" },
							{ "group2", "col1", "ghi" }, 
							{ "group2", "col2", "jkl" }
				});

		Cast cast = new Cast (rs, "group", "col", "var");
		
		assertEquals (3, cast.getNumCols());
		
		Map<String, Integer> idx = new HashMap<String, Integer>();
		for (int i = 0; i < cast.getNumCols(); ++i) idx.put (cast.getColumnName(i), i);
		assertTrue (idx.containsKey("group"));
		assertTrue (idx.containsKey("col1"));
		assertTrue (idx.containsKey("col2"));
		
		Record r;
		r = cast.getNext();
		assertEquals ("group1", r.getValue(idx.get("group")));
		assertEquals ("abc", r.getValue(idx.get("col1")));
		assertEquals ("def", r.getValue(idx.get("col2")));
		
		r = cast.getNext();
		assertEquals ("group2", r.getValue(idx.get("group")));
		assertEquals ("ghi", r.getValue(idx.get("col1")));
		assertEquals ("jkl", r.getValue(idx.get("col2")));

		r = cast.getNext();
		assertNull (r);
	}

	public void testCastMultiKey() throws RecordStreamException
	{
		RecordStream rs = new MockRecordStream (
				new String[] { "group", "subgroup", "col", "var" },
				new Object[][] 	{
							{ "group0", "subgroup1", "col1", "abc" },
							{ "group0", "subgroup1", "col2", "def" },
							{ "group0", "subgroup2", "col1", "ghi" }, 
							{ "group0", "subgroup2", "col2", "jkl" }
				});

		Cast cast = new Cast (rs, new String[] { "group", "subgroup" }, "col", "var");
		
		assertEquals (4, cast.getNumCols());
		
		Map<String, Integer> idx = new HashMap<String, Integer>();
		for (int i = 0; i < cast.getNumCols(); ++i) idx.put (cast.getColumnName(i), i);
		assertTrue (idx.containsKey("group"));
		assertTrue (idx.containsKey("subgroup"));
		assertTrue (idx.containsKey("col1"));
		assertTrue (idx.containsKey("col2"));
		
		Record r;
		r = cast.getNext();
		assertEquals ("group0", r.getValue(idx.get("group")));
		assertEquals ("subgroup1", r.getValue(idx.get("subgroup")));
		assertEquals ("abc", r.getValue(idx.get("col1")));
		assertEquals ("def", r.getValue(idx.get("col2")));
		
		r = cast.getNext();
		assertEquals ("group0", r.getValue(idx.get("group")));
		assertEquals ("subgroup2", r.getValue(idx.get("subgroup")));
		assertEquals ("ghi", r.getValue(idx.get("col1")));
		assertEquals ("jkl", r.getValue(idx.get("col2")));

		r = cast.getNext();
		assertNull (r);
	}

}
