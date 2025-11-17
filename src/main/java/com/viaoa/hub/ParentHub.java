/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.hub;

/**
 * Specialized {@link DetailHub} used to navigate *upward* in a recursive
 * or bidirectional relationship to represent a parent reference.
 *
 * <p>{@code ParentHub} provides a mirrored view of a “parent” object
 * in situations where a model defines both a forward and reverse link.
 * For example, if an {@code ExamItem} belongs to an {@code Exam}, the
 * {@code hubExamItem} can expose a {@code ParentHub<Exam>} for the
 * parent relationship.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<ExamItem> hubExamItems = new Hub<>(ExamItem.class);
 * ParentHub<Exam> hubExam = new ParentHub<>(hubExamItems, "exam");
 * }</pre>
 * The {@code hubExam} always points to the {@code Exam} referenced by the
 * active {@code ExamItem}.
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Inherits all functionality from {@link DetailHub} but semantically
 *       represents the reverse (parent) direction of a link.</li>
 *   <li>Useful for UI binding and navigation back to a master object when
 *       working within detail contexts.</li>
 *   <li>Created automatically in some OA code-generation cases where
 *       reverse navigation is required.</li>
 * </ul>
 */
public class ParentHub<TYPE> extends DetailHub<TYPE> {

    public ParentHub(Hub hubMaster, String propertyPath) {
        super(hubMaster, propertyPath);
    }
}

