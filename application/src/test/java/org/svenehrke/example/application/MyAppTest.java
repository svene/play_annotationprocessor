package org.svenehrke.example.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MyAppTest {
	@Test
	void name() {
		Person person = new PersonBuilder().setName("sven").setAge(21).build();
		assertThat(person.getName()).isEqualTo("sven");
		assertThat(person.getAge()).isEqualTo(21);
	}
}
