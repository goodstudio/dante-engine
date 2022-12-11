/*
 * Copyright (c) 2020-2030 ZHENGGENGWEI(码匠君)<herodotus@aliyun.com>
 *
 * Dante Engine licensed under the Apache License, Version 2.0 (the "License");
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
 * Dante Engine 采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改 Dante Cloud 源码头部的版权声明。
 * 3.请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://gitee.com/herodotus/dante-engine
 * 5.在修改包名，模块名称，项目代码等时，请注明软件出处 https://gitee.com/herodotus/dante-engine
 * 6.若您的项目无法满足以上几点，可申请商业授权
 */

package cn.herodotus.engine.message.information.service;

import cn.herodotus.engine.data.core.repository.BaseRepository;
import cn.herodotus.engine.data.core.service.BaseLayeredService;
import cn.herodotus.engine.message.information.entity.PersonalContact;
import cn.herodotus.engine.message.information.entity.PersonalDialogue;
import cn.herodotus.engine.message.information.entity.PersonalDialogueDetail;
import cn.herodotus.engine.message.information.repository.PersonalContactRepository;
import cn.herodotus.engine.message.information.repository.PersonalDialogueRepository;
import com.google.common.collect.Collections2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Description: PersonalContactService </p>
 *
 * @author : gengwei.zheng
 * @date : 2022/12/7 22:09
 */
@Service
public class PersonalContactService extends BaseLayeredService<PersonalContact, String> {

    private final PersonalContactRepository personalContactRepository;
    private final PersonalDialogueRepository personalDialogueRepository;

    public PersonalContactService(PersonalContactRepository personalContactRepository,
                                  PersonalDialogueRepository personalDialogueRepository) {
        this.personalContactRepository = personalContactRepository;
        this.personalDialogueRepository = personalDialogueRepository;
    }

    @Override
    public BaseRepository<PersonalContact, String> getRepository() {
        return personalContactRepository;
    }

    public List<PersonalContact> createContact(PersonalDialogue dialogue, PersonalDialogueDetail dialogueDetail) {
        PersonalContact contact = new PersonalContact();
        contact.setDialogue(dialogue);
        contact.setSenderId(dialogueDetail.getSenderId());
        contact.setReceiverId(dialogueDetail.getReceiverId());
        contact.setReceiverName(dialogueDetail.getReceiverName());

        PersonalContact reverseContext = new PersonalContact();
        reverseContext.setDialogue(dialogue);
        reverseContext.setSenderId(dialogueDetail.getReceiverId());
        reverseContext.setReceiverId(dialogueDetail.getSenderId());
        reverseContext.setReceiverName(dialogueDetail.getSenderName());

        List<PersonalContact> personalContacts = new ArrayList<>();
        personalContacts.add(contact);
        personalContacts.add(reverseContext);

        return this.saveAll(personalContacts);
    }
}
