package nl.helixsoft.recordstream;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrap a record stream, while filtering out records that do not meet a certain criterion
 */
public class Filter implements RecordStream
{
	/**
	 * Test a record, if accept returns false the record will be filtered out.
	 */
	public interface FilterFunc
	{
		public boolean accept (Record r);
	}
	
	public static class FieldInSet implements FilterFunc
	{
		private Set<String> allowedVals = new HashSet<String>();
		private final String field;
		private int idx = -1;
		
		public FieldInSet (String field, String[] set)
		{
			this.field = field;
			for (String s : set) allowedVals.add(s);
		}
		
		public boolean accept (Record r)
		{
			// find column index if needed
			if (idx == -1)
			{
				for (idx = 0; idx < r.getParent().getNumCols(); ++idx)
				{
					if (field.equals (r.getParent().getColumnName(idx)))
							break;
				}
			}
			
			
			String val = "" + r.getValue(idx);
			return allowedVals.contains(val);
		}
	}
	
	private final RecordStream parent;
	private final FilterFunc func;
	
	public Filter (RecordStream parent, FilterFunc func)
	{
		this.parent = parent;
		this.func = func;
	}
	
	@Override
	public int getNumCols()
	{
		return parent.getNumCols();
	}

	@Override
	public String getColumnName(int i)
	{
		return parent.getColumnName(i);
	}

	@Override
	public Record getNext() throws RecordStreamException
	{
		while (true)
		{
			Record result = parent.getNext();
			if (result == null) return null; // end of stream
			if (func.accept(result)) return result; // return accepted record
			// not accepted -> try next
		}
	}

	@Override
	public int getColumnIndex(String name) 
	{
		return parent.getColumnIndex(name);
	}
}
