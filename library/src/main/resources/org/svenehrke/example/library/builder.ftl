<#if PN??>
package ${PN};

</#if>
public class ${CN} {
    private ${SCN} object = new ${SCN}();

    public ${SCN} build() {
		return object;
	}

<#list SL as it>
	public ${CN} ${it.MN}(${it.AT} value) {
		object.${it.MN}(value);
		return this;
	}
</#list>
}
