package org.jetbrains.kotlin.core.tests.diagnostics;

import com.intellij.openapi.util.Condition;

public class AdditionalConditions {

	public static Condition<Object> TRUE = new Condition<Object>() {
		@Override
		public boolean value(final Object object) {
			return true;
		}
	};
	
	public static Condition<Object> FALSE = new Condition<Object>() {
		@Override
		public boolean value(final Object object) {
			return false;
		}
	};
	
	@SuppressWarnings("unchecked")
	public static <T> Condition<T> alwaysTrue() {
		return (Condition<T>) TRUE;
	}
	
	private static class And<T> implements Condition<T>  {
		private final Condition<T> t1;
		private final Condition<T> t2;

		public And(final Condition<T> t1, final Condition<T> t2) {
			this.t1 = t1;
			this.t2 = t2;
		}

		@Override
		public boolean value(final T object) {
			return t1.value(object) && t2.value(object);
		}
	}
	
	private static class Or<T> implements Condition<T>  {
		private final Condition<T> t1;
		private final Condition<T> t2;

		public Or(final Condition<T> t1, final Condition<T> t2) {
			this.t1 = t1;
			this.t2 = t2;
		}

		@Override
		public boolean value(final T object) {
			return t1.value(object) || t2.value(object);
		}
	}
	
	private static class Not<T> implements Condition<T> {
		private final Condition<T> myCondition;

		public Not(Condition<T> condition) {
			myCondition = condition;
		}

		@Override
		public boolean value(T value) {
			return !myCondition.value(value);
		}
	}
	
	public static <T> Condition<T> or(Condition<T> c1, Condition<T> c2) {
		return new Or<T>(c1, c2);
	}
	
	public static <T> Condition<T> not(Condition<T> c) {
		return new Not<T>(c);
	}
	
	public static <T> Condition<T> and(Condition<T> c1, Condition<T> c2) {
		return new And<T>(c1, c2);
	}
}
