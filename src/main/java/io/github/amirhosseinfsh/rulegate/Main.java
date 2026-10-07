package io.github.amirhosseinfsh.rulegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.domain.fact.FactDto;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import io.github.amirhosseinfsh.rulegate.test.Address;
import io.github.amirhosseinfsh.rulegate.test.VahedBimeh;
import io.github.amirhosseinfsh.rulegate.test.YESNO;

import java.time.LocalDateTime;
import java.util.Set;

public class Main {


    public static void main(String[] args) throws JsonProcessingException {
        DroolsHelper droolsHelper = new DroolsHelper();
        VahedBimeh vahedBiemh = new VahedBimeh();
        vahedBiemh.setId(1L);
        vahedBiemh.setYn(YESNO.NO);
        vahedBiemh.setDateTime(LocalDateTime.now());
        vahedBiemh.setNaam("Ali");
        Address address1 = new Address();
        Address address2 = new Address();
        address1.setId(2L);
        address1.setVahedBiemh(vahedBiemh);
        address2.setId(2L);
        address2.setVahedBiemh(vahedBiemh);
        vahedBiemh.setAddressList(Set.of(address1, address2));
        FactDto factDto = droolsHelper.toFactDto(vahedBiemh);

        var mapper = new ObjectMapper();
        var data = mapper.writeValueAsString(factDto);
        System.out.println(data);
    }

}
