/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Test;

public class KotlinAutoImportTest extends KotlinAutoImportTestCase {

	@Test
	public void standardEnumMapAutoImport() {
		doAutoTest();
	}

	@Test
	public void packageArrayListAutoImport() {
		doAutoTest();
	}
	
	@Test
	public void sameProjectJavaClassAutoImport() {
		doAutoTest();
	}
	
	@Test
	public void localJavaEnumAutoImport() {
		doAutoTest();
	}
	
	@Test
	public void localJavaClassAutoImport() {
		doAutoTest();
	}
	
	@Test
	public void localJavaInterfaceAutoImport() {
		doAutoTest();
	}
	
	@Test
	public void importClassWithExistingPackageKeyword() {
		doAutoTest();
	}
	
	@Test
	public void importOnlyUnresolvedReferenceExpressions() {
		doAutoTest();
	}
	
	@Test
	public void importWithExtraBreakline() {
		doAutoTest();
	}
	
	@Test
	public void importWithExtraBreaklineWithoutPackage() {
		doAutoTest();
	}
	
	@Test
	public void oneStandardVectorAutoImport() {
		doAutoTest();
	}
}
