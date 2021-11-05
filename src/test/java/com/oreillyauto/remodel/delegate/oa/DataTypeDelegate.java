package com.oreillyauto.remodel.delegate.oa;

import com.oreillyauto.remodel.delegate.ModelDelegate;
import com.oreillyauto.remodel.model.oa.DataType;

public class DataTypeDelegate {

	private static final Object Lock = new Object();

	public static DataType getDataType(int type) {
		DataType dt = ModelDelegate.getDataTypes().find(DataType.P_Type, type);
		return dt;
	}

	public static void assignDefaultValues() {
		getDataType(DataType.TYPE_shortType).setIsNumeric(true);
		getDataType(DataType.TYPE_intType).setIsNumeric(true);
		getDataType(DataType.TYPE_longType).setIsNumeric(true);

		getDataType(DataType.TYPE_floatType).setIsNumeric(true);
		getDataType(DataType.TYPE_floatType).setIsDecimal(true);

		getDataType(DataType.TYPE_doubleType).setIsNumeric(true);
		getDataType(DataType.TYPE_doubleType).setIsDecimal(true);

		getDataType(DataType.TYPE_Number).setIsNumeric(true);
		getDataType(DataType.TYPE_Number).setIsDecimal(true);

		getDataType(DataType.TYPE_Currency).setIsNumeric(true);
		getDataType(DataType.TYPE_Currency).setIsDecimal(true);
	}
}
