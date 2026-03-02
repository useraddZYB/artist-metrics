package com.xsj.programmerartist.metrics.bean;


import com.xsj.programmerartist.metrics.constant.Const;
import com.xsj.programmerartist.metrics.util.ControlConfig;
import com.xsj.programmerartist.metrics.util.MetricsUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author zyb
 * @Date 2024/11/27
 **/
public class Tag {
    public static final Tag EMPTY_TAG = Tag.builder();

    private List<TagEntry> value;

    private Tag() { }

    /**
     *
     * @return
     */
    public static Tag builder() {
        Tag tag = new Tag();
        tag.value = new ArrayList<>();
        return tag;
    }

    /**
     *
     * @param tagName
     * @param tagValue
     * @return
     */
    public Tag add(String tagName, Object tagValue) {
        if(null!=tagName && !"".equals(tagName) && null!=tagValue) {
            if((tagValue instanceof String) && "".equals(String.valueOf(tagValue).trim())) {
                return this;
            }

            // tag值"规整"
            tagValue = ControlConfig.getTagValByClean(tagName, tagValue);

            tagName = MetricsUtil.replaceIllegal(tagName);
            if(tagValue instanceof String) {
                tagValue = MetricsUtil.replaceIllegal((String)tagValue);
            }
            this.value.add(new TagEntry(tagName, tagValue));
        }
        return this;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        if(null==value || value.size()==0) return "";

        // 有并发问题，需要先clone再sort
        List<TagEntry> valueClone = new ArrayList<>(value);

        Collections.sort(valueClone);
        return String.join(".", valueClone.stream().map(val -> val.toString()).collect(Collectors.toList()));
    }

    /**
     *
     * @return
     */
    public Set<String> getAllTagName() {
        if(null == value) return new HashSet<>();
        return value.stream().map(val -> val.getName()).collect(Collectors.toSet());
    }

    public List<TagEntry> getValue() {
        return value;
    }

    /**
     * entry
     */
    public static class TagEntry implements Comparable<TagEntry> {

        private String name;
        private Object value;

        /**
         *
         * @param name
         * @param value
         */
        public TagEntry(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(TagEntry o) {
            return (null == this.name || null == o || null == o.getName()) ? 0 : this.name.compareTo(o.getName());
        }

        @Override
        public String toString() {
            return name + Const.Protocol.TAG_VALUE_SEP + value;
        }
    }
}
