package com.tbm.admin.model.message;

import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.view.base.ScrapUrlView;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ScrapUrlMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    List<ScrapUrlView> scrapUrlViews;
    Long adminSeq;
}
