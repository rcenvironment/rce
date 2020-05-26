window.onload = function(){
	let checkbox = document.getElementById("launchWhenStarted");
	let checkboxLinkValue = document.getElementById("checkboxLinkValue");
	/* !!! Attention when editing variable name, changes are required in SaveUserInput.java,too. @see de.rcenvironment.core.gui.introduction !!! */
	checkbox.checked = true;
	
	let applyCheckboxValue = function(){
		if (checkbox.checked){
			/* change the attribute value of the link with the id="checkLinkValue" to checked */
			checkboxLinkValue.href = "http://org.eclipse.ui.intro/saveInput?checkboxValue=checked";  
			checkboxLinkValue.click();
		}
		else{
			/* change the attribute value of the link with the id="checkLinkValue" to unchecked */
			checkboxLinkValue.href = "http://org.eclipse.ui.intro/saveInput?checkboxValue=unchecked";
			checkboxLinkValue.click();
		}
	}
	
	checkbox.onclick = function(event){
		applyCheckboxValue();
	}
	
	/* applyCheckboxValue() is executed everytime when the welcome screen is loaded */
	applyCheckboxValue();
}