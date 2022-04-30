package org.svenehrke.example.application;

import org.svenehrke.example.library.BuilderProperty;

public class Person {
	private int age;
	private String name;

	@BuilderProperty
	public void setAge(int age) {
		this.age = age;
	}

	@BuilderProperty
	public void setName(String name) {
		this.name = name;
	}
}
